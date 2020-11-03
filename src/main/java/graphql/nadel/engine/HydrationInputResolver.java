package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.dsl.ExtendedFieldDefinition;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.dsl.UnderlyingServiceHydration;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.FieldUtils;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.result.ElapsedTime;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.ListExecutionResultNode;
import graphql.nadel.result.ObjectExecutionResultNode;
import graphql.nadel.result.ResultComplexityAggregator;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.language.Field.newField;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.engine.ArtificialFieldUtils.addObjectIdentifier;
import static graphql.nadel.engine.StrategyUtil.changeFieldIdsInResultNode;
import static graphql.nadel.engine.StrategyUtil.copyFieldInformation;
import static graphql.nadel.engine.StrategyUtil.getHydrationInputNodes;
import static graphql.nadel.engine.StrategyUtil.groupNodesIntoBatchesByField;
import static graphql.nadel.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;
import static graphql.nadel.util.FpKit.filter;
import static graphql.nadel.util.FpKit.findOneOrNull;
import static graphql.nadel.util.FpKit.flatList;
import static graphql.nadel.util.FpKit.map;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.schema.GraphQLTypeUtil.unwrapNonNull;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

@Internal
public class HydrationInputResolver {

    private final OverallQueryTransformer queryTransformer = new OverallQueryTransformer();

    private final ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult();


    private final List<Service> services;
    private final GraphQLSchema overallSchema;
    private final ServiceExecutor serviceExecutor;
    private final ServiceExecutionHooks serviceExecutionHooks;

    public HydrationInputResolver(List<Service> services,
                                  GraphQLSchema overallSchema,
                                  ServiceExecutor serviceExecutor,
                                  ServiceExecutionHooks serviceExecutionHooks) {
        this.services = services;
        this.overallSchema = overallSchema;
        this.serviceExecutor = serviceExecutor;
        this.serviceExecutionHooks = serviceExecutionHooks;
    }


    public CompletableFuture<ExecutionResultNode> resolveAllHydrationInputs(ExecutionContext context,
                                                                            ExecutionResultNode node,
                                                                            Map<Service, Object> serviceContexts,
                                                                            ResultComplexityAggregator resultComplexityAggregator) {
        Set<NodeZipper<ExecutionResultNode>> hydrationInputZippers = getHydrationInputNodes(node);
        if (hydrationInputZippers.size() == 0) {
            return CompletableFuture.completedFuture(node);
        }

        List<NodeMultiZipper<ExecutionResultNode>> hydrationInputBatches = groupNodesIntoBatchesByField(hydrationInputZippers, node);

        List<CompletableFuture<List<NodeZipper<ExecutionResultNode>>>> resolvedNodeCFs = new ArrayList<>();

        for (NodeMultiZipper<ExecutionResultNode> batch : hydrationInputBatches) {
            if (isBatchHydrationField((HydrationInputNode) batch.getZippers().get(0).getCurNode())) {
                resolveInputNodesAsBatch(context, resolvedNodeCFs, batch, serviceContexts, resultComplexityAggregator);
            } else {
                resolveInputNodes(context, resolvedNodeCFs, batch, serviceContexts, resultComplexityAggregator);
            }

        }
        return Async
                .each(resolvedNodeCFs)
                .thenCompose(resolvedNodes -> {
                    NodeMultiZipper<ExecutionResultNode> multiZipper = new NodeMultiZipper<>(node, flatList(resolvedNodes), RESULT_NODE_ADAPTER);
                    ExecutionResultNode newRoot = multiZipper.toRootNode();
                    return resolveAllHydrationInputs(context, newRoot, serviceContexts, resultComplexityAggregator);
                })
                .whenComplete(this::possiblyLogException);
    }

    private void resolveInputNodes(ExecutionContext context,
                                   List<CompletableFuture<List<NodeZipper<ExecutionResultNode>>>> resolvedNodeCFs,
                                   NodeMultiZipper<ExecutionResultNode> batch, Map<Service, Object> serviceContexts,
                                   ResultComplexityAggregator resultComplexityAggregator) {
        for (NodeZipper<ExecutionResultNode> hydrationInputNodeZipper : batch.getZippers()) {
            HydrationInputNode hydrationInputNode = (HydrationInputNode) hydrationInputNodeZipper.getCurNode();
            CompletableFuture<ExecutionResultNode> executionResultNodeCompletableFuture = resolveSingleHydrationInput(context, hydrationInputNode, serviceContexts, resultComplexityAggregator);
            resolvedNodeCFs.add(executionResultNodeCompletableFuture.thenApply(newNode -> singletonList(hydrationInputNodeZipper.withNewNode(newNode))));
        }
    }

    private void resolveInputNodesAsBatch(ExecutionContext context,
                                          List<CompletableFuture<List<NodeZipper<ExecutionResultNode>>>> resolvedNodeCFs,
                                          NodeMultiZipper<ExecutionResultNode> batch,
                                          Map<Service, Object> serviceContexts,
                                          ResultComplexityAggregator resultComplexityAggregator) {
        List<NodeMultiZipper<ExecutionResultNode>> batchesWithCorrectSize = groupIntoCorrectBatchSizes(batch);
        for (NodeMultiZipper<ExecutionResultNode> oneBatch : batchesWithCorrectSize) {
            List<HydrationInputNode> batchedNodes = map(oneBatch.getZippers(), zipper -> (HydrationInputNode) zipper.getCurNode());
            CompletableFuture<List<ExecutionResultNode>> executionResultNodeCompletableFuture = resolveHydrationInputBatch(context, batchedNodes, serviceContexts, resultComplexityAggregator);
            resolvedNodeCFs.add(replaceNodesInZipper(oneBatch, executionResultNodeCompletableFuture));
        }
    }

    private Integer getDefaultBatchSize(UnderlyingServiceHydration underlyingServiceHydration) {
        GraphQLFieldDefinition graphQLFieldDefinition = null;
        String topLevelField = underlyingServiceHydration.getTopLevelField();

        if (underlyingServiceHydration.getSyntheticField() != null) {
            GraphQLFieldDefinition syntheticFieldDefinition = overallSchema.getQueryType().getFieldDefinition(underlyingServiceHydration.getSyntheticField());
            if(syntheticFieldDefinition == null) {
                return null;
            }
            GraphQLObjectType syntheticFieldDefinitionType = (GraphQLObjectType) syntheticFieldDefinition.getType();
            graphQLFieldDefinition = syntheticFieldDefinitionType.getFieldDefinition(underlyingServiceHydration.getTopLevelField());
        } else {
            graphQLFieldDefinition = overallSchema.getQueryType().getFieldDefinition(topLevelField);
        }
        // the field we use to hydrate doesn't need to be exposed, therefore can be null
        if (graphQLFieldDefinition == null) {
            return null;
        }
        FieldDefinition fieldDefinition = graphQLFieldDefinition.getDefinition();
        if (!(fieldDefinition instanceof ExtendedFieldDefinition)) {
            return null;
        }
        return ((ExtendedFieldDefinition) fieldDefinition).getDefaultBatchSize();
    }

    private List<NodeMultiZipper<ExecutionResultNode>> groupIntoCorrectBatchSizes(NodeMultiZipper<ExecutionResultNode> batch) {
        HydrationInputNode node = (HydrationInputNode) batch.getZippers().get(0).getCurNode();

        Integer batchSize = node.getHydrationTransformation().getUnderlyingServiceHydration().getBatchSize();
        if (batchSize == null) {
            batchSize = getDefaultBatchSize(node.getHydrationTransformation().getUnderlyingServiceHydration());
        }
        if (batchSize == null) {
            return singletonList(batch);
        }
        List<NodeMultiZipper<ExecutionResultNode>> result = new ArrayList<>();
        int counter = 0;
        List<NodeZipper<ExecutionResultNode>> currentBatch = new ArrayList<>();
        for (NodeZipper<ExecutionResultNode> zipper : batch.getZippers()) {
            currentBatch.add(zipper);
            counter++;
            if (counter == batchSize) {
                result.add(new NodeMultiZipper<>(batch.getCommonRoot(), currentBatch, RESULT_NODE_ADAPTER));
                counter = 0;
                currentBatch = new ArrayList<>();
            }
        }
        if (currentBatch.size() > 0) {
            result.add(new NodeMultiZipper<>(batch.getCommonRoot(), currentBatch, RESULT_NODE_ADAPTER));
        }
        return result;
    }


    private boolean isBatchHydrationField(HydrationInputNode hydrationInputNode) {
        HydrationTransformation hydrationTransformation = hydrationInputNode.getHydrationTransformation();
        Service service = getService(hydrationTransformation.getUnderlyingServiceHydration());

        String syntheticFieldName = hydrationTransformation.getUnderlyingServiceHydration().getSyntheticField();
        String topLevelFieldName = hydrationTransformation.getUnderlyingServiceHydration().getTopLevelField();

        GraphQLFieldDefinition topLevelFieldDefinition;
        if (syntheticFieldName == null) {
            topLevelFieldDefinition = service.getUnderlyingSchema().getQueryType().getFieldDefinition(topLevelFieldName);
        } else {
            topLevelFieldDefinition = ((GraphQLObjectType)service.getUnderlyingSchema().getQueryType().getFieldDefinition(syntheticFieldName).getType()).getFieldDefinition(topLevelFieldName);
        }

        return isList(unwrapNonNull(topLevelFieldDefinition.getType()));
    }


    private CompletableFuture<List<NodeZipper<ExecutionResultNode>>> replaceNodesInZipper(NodeMultiZipper<ExecutionResultNode> batch,
                                                                                          CompletableFuture<List<ExecutionResultNode>> executionResultNodeCompletableFuture) {
        return executionResultNodeCompletableFuture.thenApply(executionResultNodes -> {
            List<NodeZipper<ExecutionResultNode>> newZippers = new ArrayList<>();
            List<NodeZipper<ExecutionResultNode>> zippers = batch.getZippers();
            for (int i = 0; i < executionResultNodes.size(); i++) {
                NodeZipper<ExecutionResultNode> zipper = zippers.get(i);
                NodeZipper<ExecutionResultNode> newZipper = zipper.withNewNode(executionResultNodes.get(i));
                newZippers.add(newZipper);
            }
            return newZippers;
        });
    }

    private CompletableFuture<ExecutionResultNode> resolveSingleHydrationInput(ExecutionContext executionContext,
                                                                               HydrationInputNode hydrationInputNode,
                                                                               Map<Service, Object> serviceContexts,
                                                                               ResultComplexityAggregator resultComplexityAggregator) {
        HydrationTransformation hydrationTransformation = hydrationInputNode.getHydrationTransformation();

        Field originalField = hydrationTransformation.getOriginalField();
        UnderlyingServiceHydration underlyingServiceHydration = hydrationTransformation.getUnderlyingServiceHydration();
        String topLevelFieldName = underlyingServiceHydration.getTopLevelField();
        Service service = getService(underlyingServiceHydration);

        Field topLevelField = createSingleHydrationTopLevelField(hydrationInputNode,
                originalField.getSelectionSet(),
                underlyingServiceHydration,
                topLevelFieldName,
                underlyingServiceHydration.getSyntheticField(),
                originalField);
        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

        boolean isSyntheticHydration = underlyingServiceHydration.getSyntheticField() != null;
        QueryTransformationResult queryTransformationResult = queryTransformer
                .transformHydratedTopLevelField(
                        executionContext,
                        service.getUnderlyingSchema(),
                        operationName,
                        operation,
                        topLevelField,
                        topLevelFieldType,
                        serviceExecutionHooks,
                        service,
                        serviceContexts.get(service),
                        isSyntheticHydration
                );


        CompletableFuture<RootExecutionResultNode> serviceResult = serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation,
                        serviceContexts.get(service), true);

        return serviceResult
                .thenApply(resultNode -> convertSingleHydrationResultIntoOverallResult(executionContext.getExecutionId(),
                        hydrationInputNode,
                        hydrationTransformation,
                        resultNode,
                        hydrationInputNode.getNormalizedField(),
                        queryTransformationResult,
                        getNadelContext(executionContext),
                        resultComplexityAggregator
                ))
                .whenComplete(this::possiblyLogException);

    }

    private Field createSingleHydrationTopLevelField(HydrationInputNode hydrationInputNode,
                                                     SelectionSet selectionSet,
                                                     UnderlyingServiceHydration underlyingServiceHydration,
                                                     String topLevelFieldName,
                                                     String syntheticFieldName,
                                                     Field originalField) {
        List<RemoteArgumentDefinition> arguments = underlyingServiceHydration.getArguments();
        RemoteArgumentDefinition argumentFromSourceObject = findOneOrNull(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.OBJECT_FIELD);
        List<RemoteArgumentDefinition> extraArguments = filter(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.FIELD_ARGUMENT);

        Object value = hydrationInputNode.getCompletedValue();
        Argument argumentAstFromSourceObject = Argument.newArgument()
                .name(argumentFromSourceObject.getName())
                .value(new StringValue(value.toString()))
                .build();

        List<Argument> allArguments = new ArrayList<>();
        allArguments.add(argumentAstFromSourceObject);

        Map<String, Argument> originalArgumentsByName = FpKit.getByName(originalField.getArguments(), Argument::getName);
        for (RemoteArgumentDefinition argumentDefinition : extraArguments) {
            if (originalArgumentsByName.containsKey(argumentDefinition.getName())) {
                allArguments.add(originalArgumentsByName.get(argumentDefinition.getName()));
            }
        }


        Field topLevelField = newField(topLevelFieldName)
                .selectionSet(selectionSet)
                .arguments(allArguments)
                .additionalData(NodeId.ID, FieldUtils.randomNodeId())
                .build();

        if (syntheticFieldName == null) {
            return topLevelField;
        }

        Field syntheticField = newField(syntheticFieldName)
                .selectionSet(newSelectionSet().selection(topLevelField).build())
                .additionalData(NodeId.ID, FieldUtils.randomNodeId())
                .build();
        return syntheticField;
    }

    private ExecutionResultNode convertSingleHydrationResultIntoOverallResult(ExecutionId executionId,
                                                                              HydrationInputNode hydrationInputNode,
                                                                              HydrationTransformation hydrationTransformation,
                                                                              RootExecutionResultNode rootResultNode,
                                                                              NormalizedQueryField rootNormalizedField,
                                                                              QueryTransformationResult queryTransformationResult,
                                                                              NadelContext nadelContext,
                                                                              ResultComplexityAggregator resultComplexityAggregator
    ) {

        Map<String, FieldTransformation> transformationByResultField = queryTransformationResult.getFieldIdToTransformation();
        Map<String, String> typeRenameMappings = queryTransformationResult.getTypeRenameMappings();
        assertTrue(rootResultNode.getChildren().size() == 1, () -> "expected rootResultNode to only have 1 child.");

        ExecutionResultNode root = rootResultNode.getChildren().get(0);
        if (hydrationTransformation.getUnderlyingServiceHydration().getSyntheticField() != null && root.getChildren().size() > 0) {
            assertTrue(root.getChildren().size() == 1, () -> "expected synthetic field to only have 1 topLevelField child.");
            root = root.getChildren().get(0);
        }

        ExecutionResultNode firstTopLevelResultNode = serviceResultNodesToOverallResult
                .convertChildren(executionId,
                        root,
                        rootNormalizedField,
                        overallSchema,
                        hydrationInputNode,
                        true,
                        false,
                        transformationByResultField,
                        typeRenameMappings,
                        nadelContext,
                        queryTransformationResult.getRemovedFieldMap());
        String serviceName = hydrationTransformation.getUnderlyingServiceHydration().getServiceName();
        resultComplexityAggregator.incrementServiceNodeCount(serviceName, firstTopLevelResultNode.getTotalNodeCount());
        firstTopLevelResultNode = firstTopLevelResultNode.withNewErrors(rootResultNode.getErrors());
        firstTopLevelResultNode = StrategyUtil.copyFieldInformation(hydrationInputNode, firstTopLevelResultNode);

        return changeFieldIdsInResultNode(firstTopLevelResultNode, NodeId.getId(hydrationTransformation.getOriginalField()));
    }

    private CompletableFuture<List<ExecutionResultNode>> resolveHydrationInputBatch(ExecutionContext executionContext,
                                                                                    List<HydrationInputNode> hydrationInputs,
                                                                                    Map<Service, Object> serviceContexts,
                                                                                    ResultComplexityAggregator resultComplexityAggregator) {

        List<HydrationTransformation> hydrationTransformations = map(hydrationInputs, HydrationInputNode::getHydrationTransformation);


        HydrationTransformation hydrationTransformation = hydrationTransformations.get(0);
        Field originalField = hydrationTransformation.getOriginalField();
        UnderlyingServiceHydration underlyingServiceHydration = hydrationTransformation.getUnderlyingServiceHydration();
        Service service = getService(underlyingServiceHydration);

        Field topLevelField = createBatchHydrationTopLevelField(executionContext,
                hydrationInputs,
                originalField,
                underlyingServiceHydration);
        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

        boolean isSyntheticHydration = underlyingServiceHydration.getSyntheticField() != null;
        QueryTransformationResult queryTransformationResult = queryTransformer
                .transformHydratedTopLevelField(
                        executionContext,
                        service.getUnderlyingSchema(),
                        operationName, operation,
                        topLevelField,
                        topLevelFieldType,
                        serviceExecutionHooks,
                        service,
                        serviceContexts.get(service),
                        isSyntheticHydration
                );


        return serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation, serviceContexts.get(service), true)
                .thenApply(resultNode -> convertHydrationBatchResultIntoOverallResult(executionContext, hydrationInputs, resultNode, queryTransformationResult, resultComplexityAggregator))
                .whenComplete(this::possiblyLogException);

    }

    private Field createBatchHydrationTopLevelField(ExecutionContext executionContext,
                                                    List<HydrationInputNode> hydrationInputs,
                                                    Field originalField,
                                                    UnderlyingServiceHydration underlyingServiceHydration) {

        String topLevelFieldName = underlyingServiceHydration.getTopLevelField();
        String syntheticFieldName = underlyingServiceHydration.getSyntheticField();
        List<RemoteArgumentDefinition> arguments = underlyingServiceHydration.getArguments();
        RemoteArgumentDefinition argumentFromSourceObject = findOneOrNull(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.OBJECT_FIELD);
        List<RemoteArgumentDefinition> extraArguments = filter(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.FIELD_ARGUMENT);

        List<Value> values = new ArrayList<>();
        for (ExecutionResultNode hydrationInputNode : hydrationInputs) {
            Object value = hydrationInputNode.getCompletedValue();
            values.add(StringValue.newStringValue(value.toString()).build());
        }
        Argument argumentAstFromSourceObject = Argument.newArgument().name(argumentFromSourceObject.getName()).value(new ArrayValue(values)).build();
        List<Argument> allArguments = new ArrayList<>();
        allArguments.add(argumentAstFromSourceObject);

        Map<String, Argument> originalArgumentsByName = FpKit.getByName(originalField.getArguments(), Argument::getName);
        for (RemoteArgumentDefinition argumentDefinition : extraArguments) {
            if (originalArgumentsByName.containsKey(argumentDefinition.getName())) {
                allArguments.add(originalArgumentsByName.get(argumentDefinition.getName()));
            }
        }

        Field topLevelField = newField(topLevelFieldName)
                .selectionSet(originalField.getSelectionSet())
                .additionalData(NodeId.ID, FieldUtils.randomNodeId())
                .arguments(allArguments)
                .build();
        topLevelField = addObjectIdentifier(getNadelContext(executionContext), topLevelField, underlyingServiceHydration.getObjectIdentifier());

        if (syntheticFieldName == null) {
            return topLevelField;
        }

        Field syntheticField = newField(syntheticFieldName)
                .selectionSet(newSelectionSet().selection(topLevelField).build())
                .additionalData(NodeId.ID, FieldUtils.randomNodeId())
                .build();
        return syntheticField;
    }


    private List<ExecutionResultNode> convertHydrationBatchResultIntoOverallResult(ExecutionContext executionContext,
                                                                                   List<HydrationInputNode> hydrationInputNodes,
                                                                                   RootExecutionResultNode rootResultNode,
                                                                                   QueryTransformationResult queryTransformationResult,
                                                                                   ResultComplexityAggregator resultComplexityAggregator) {
        boolean isSyntheticHydration = hydrationInputNodes.get(0).getHydrationTransformation().getUnderlyingServiceHydration().getSyntheticField() != null;

        ExecutionResultNode root = rootResultNode.getChildren().get(0);
        if (!(root instanceof LeafExecutionResultNode) && isSyntheticHydration) {
            root = root.getChildren().get(0);
        }
        if (root instanceof LeafExecutionResultNode) {
            // we only expect a null value here
            assertTrue(root.isNullValue());
            List<ExecutionResultNode> result = new ArrayList<>();
            boolean first = true;
            for (HydrationInputNode hydrationInputNode : hydrationInputNodes) {
                ExecutionResultNode resultNode = createNullValue(hydrationInputNode);
                if (first) {
                    resultNode = resultNode.withNewErrors(rootResultNode.getErrors());
                    first = false;
                }
                result.add(resultNode);
            }
            return result;
        }
        assertTrue(root instanceof ListExecutionResultNode, () -> "expect a list result from the underlying service for batched hydration");
        ListExecutionResultNode listResultNode = (ListExecutionResultNode) root;
        List<ExecutionResultNode> resolvedNodes = listResultNode.getChildren();

        List<ExecutionResultNode> result = new ArrayList<>();
        Map<String, FieldTransformation> transformationByResultField = queryTransformationResult.getFieldIdToTransformation();
        Map<String, String> typeRenameMappings = queryTransformationResult.getTypeRenameMappings();

        boolean first = true;
        for (HydrationInputNode hydrationInputNode : hydrationInputNodes) {
            ObjectExecutionResultNode matchingResolvedNode = findMatchingResolvedNode(executionContext, hydrationInputNode, resolvedNodes);
            ExecutionResultNode resultNode;
            if (matchingResolvedNode != null) {
                ExecutionResultNode overallResultNode = serviceResultNodesToOverallResult.convertChildren(
                        executionContext.getExecutionId(),
                        matchingResolvedNode,
                        hydrationInputNode.getNormalizedField(),
                        overallSchema,
                        hydrationInputNode,
                        true,
                        true,
                        transformationByResultField,
                        typeRenameMappings,
                        getNadelContext(executionContext),
                        queryTransformationResult.getRemovedFieldMap());

                String serviceName = hydrationInputNode.getHydrationTransformation().getUnderlyingServiceHydration().getServiceName();
                int nodeCount = overallResultNode.getTotalNodeCount();
                resultComplexityAggregator.incrementServiceNodeCount(serviceName, nodeCount);

                resultNode = copyFieldInformation(hydrationInputNode, overallResultNode);
            } else {
                resultNode = createNullValue(hydrationInputNode);
            }
            if (first) {
                resultNode = resultNode.withNewErrors(rootResultNode.getErrors());
                first = false;
            }
            result.add(resultNode);
        }
        return result;

    }

    private LeafExecutionResultNode createNullValue(HydrationInputNode inputNode) {
        ElapsedTime elapsedTime = inputNode.getElapsedTime();
        return LeafExecutionResultNode.newLeafExecutionResultNode()
                .objectType(inputNode.getObjectType())
                .alias(inputNode.getAlias())
                .fieldIds(inputNode.getFieldIds())
                .executionPath(inputNode.getExecutionPath())
                .fieldDefinition(inputNode.getFieldDefinition())
                .completedValue(null)
                .elapsedTime(elapsedTime)
                .build();
    }

    private ObjectExecutionResultNode findMatchingResolvedNode(ExecutionContext executionContext, HydrationInputNode inputNode, List<ExecutionResultNode> resolvedNodes) {
        NadelContext nadelContext = getNadelContext(executionContext);
        String objectIdentifier = nadelContext.getObjectIdentifierAlias();
        String inputNodeId = (String) inputNode.getCompletedValue();
        for (ExecutionResultNode resolvedNode : resolvedNodes) {
            LeafExecutionResultNode idNode = getFieldByResultKey((ObjectExecutionResultNode) resolvedNode, objectIdentifier);
            assertNotNull(idNode, () -> String.format("no value found for object identifier: %s", objectIdentifier));
            Object id = idNode.getCompletedValue();
            assertNotNull(id, () -> "object identifier is null");
            if (id.equals(inputNodeId)) {
                return (ObjectExecutionResultNode) resolvedNode;
            }
        }
        return null;
    }


    private LeafExecutionResultNode getFieldByResultKey(ObjectExecutionResultNode node, String resultKey) {
        return (LeafExecutionResultNode) findOneOrNull(node.getChildren(), child -> child.getResultKey().equals(resultKey));
    }


    @SuppressWarnings("unused")
    private <T> void possiblyLogException(T result, Throwable exception) {
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Service getService(UnderlyingServiceHydration underlyingServiceHydration) {
        return FpKit.findOne(services, service -> service.getName().equals(underlyingServiceHydration.getServiceName())).get();
    }

    private String buildOperationName(Service service, ExecutionContext executionContext) {
        // to help with downstream debugging we put our name and their name in the operation
        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        if (nadelContext.getOriginalOperationName() != null) {
            return format("nadel_2_%s_%s", service.getName(), nadelContext.getOriginalOperationName());
        } else {
            return format("nadel_2_%s", service.getName());
        }
    }

    private NadelContext getNadelContext(ExecutionContext executionContext) {
        return (NadelContext) executionContext.getContext();
    }

}
