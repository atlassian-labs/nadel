package graphql.nadel.engine;

import graphql.GraphQLException;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

    private Integer getDefaultBatchSize(UnderlyingServiceHydration underlyingServiceHydration, boolean isSyntheticHydration) {
        GraphQLFieldDefinition  graphQLFieldDefinition = null;
        if (isSyntheticHydration) {
            String syntheticFieldName = underlyingServiceHydration.getSyntheticField();

            Optional<GraphQLFieldDefinition> topLevelFieldDef = overallSchema.getObjectType("Query").getFieldDefinitions().stream()
                    .filter(fieldDef -> fieldDef.getName().equals(underlyingServiceHydration.getTopLevelField()))
                    .findFirst();
            if (topLevelFieldDef.isPresent()) {
                graphQLFieldDefinition = ((GraphQLObjectType)topLevelFieldDef.get().getType()).getFieldDefinition(syntheticFieldName);
            }

        } else {
            String topLevelField = underlyingServiceHydration.getTopLevelField();
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

        boolean isSyntheticHydration = !node.getHydrationTransformation().getUnderlyingServiceHydration().getSyntheticField().isEmpty();
        Integer batchSize = node.getHydrationTransformation().getUnderlyingServiceHydration().getBatchSize();
        if (batchSize == null) {
            batchSize = getDefaultBatchSize(node.getHydrationTransformation().getUnderlyingServiceHydration(), isSyntheticHydration);
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
        if (syntheticFieldName.isEmpty()) {
            String topLevelFieldName = hydrationTransformation.getUnderlyingServiceHydration().getTopLevelField();
            GraphQLFieldDefinition topLevelFieldDefinition = service.getUnderlyingSchema().getQueryType().getFieldDefinition(topLevelFieldName);
            return isList(unwrapNonNull(topLevelFieldDefinition.getType()));
        }

        GraphQLFieldDefinition syntheticFieldDefinition = getSyntheticFieldDefinition(syntheticFieldName, service);
        return isList(unwrapNonNull(syntheticFieldDefinition.getType()));
    }


    private GraphQLFieldDefinition getSyntheticFieldDefinition(String syntheticFieldName, Service service) {
        return service.getUnderlyingSchema().getAllTypesAsList().stream()
                .filter(graphQLNamedType -> graphQLNamedType instanceof GraphQLObjectType)
                .flatMap(type -> ((GraphQLObjectType) type).getFieldDefinitions().stream())
                .filter(graphQLFieldDefinition -> graphQLFieldDefinition.getName().equals(syntheticFieldName))
                .findFirst()
                .orElseThrow(() -> new GraphQLException(String.format("No field definition found for synthetic field %s.", syntheticFieldName)));
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

        Field topLevelField;
        GraphQLCompositeType topLevelFieldType;

        boolean isSyntheticHydration = !underlyingServiceHydration.getSyntheticField().isEmpty();
        if (isSyntheticHydration) {
            Field syntheticField = createSyntheticField(originalField, underlyingServiceHydration, hydrationInputNode);
            SelectionSet newSelectionSet = newSelectionSet().selection(syntheticField).build();

            topLevelField = createSingleHydrationTopLevelField(hydrationInputNode, newSelectionSet, underlyingServiceHydration, topLevelFieldName, true);
            topLevelFieldType = topLevelfieldTypeForSyntheticHydration(topLevelFieldName,underlyingServiceHydration.getSyntheticField(),  service);
        } else {
            topLevelField = createSingleHydrationTopLevelField(hydrationInputNode, originalField.getSelectionSet(), underlyingServiceHydration, topLevelFieldName, false);
            topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());
        }

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

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
                        serviceContexts.get(service)
                );


        CompletableFuture<RootExecutionResultNode> serviceResult = serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation,
                        serviceContexts.get(service), true);

        return serviceResult
                .thenApply(resultNode -> isSyntheticHydration ? applySyntheticFieldChange(resultNode, getNadelContext(executionContext), underlyingServiceHydration.getSyntheticField(), false) : resultNode)
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

    // We get the field type in overall schema; otherwise get the field type in underlying schema if absent
    private GraphQLCompositeType topLevelfieldTypeForSyntheticHydration(String topLevelFieldName, String syntheticFieldName, Service service) {
        GraphQLFieldDefinition overallType = overallSchema.getObjectType("Query").getFieldDefinition(topLevelFieldName);
        if (overallType != null && ((GraphQLObjectType) overallType.getType()).getFieldDefinition(syntheticFieldName) != null) {
            return (GraphQLCompositeType) unwrapAll(overallType.getType());
        }
        overallType = service.getUnderlyingSchema().getQueryType().getFieldDefinition(topLevelFieldName);
        assertNotNull(overallType, () -> String.format("Field type does not exist for %s in overall schema or underlying schema.", topLevelFieldName));
        return (GraphQLCompositeType) unwrapAll(overallType.getType());
    }

    private Field createSyntheticField(Field originalField, UnderlyingServiceHydration underlyingServiceHydration, HydrationInputNode hydrationInputNode) {
        RemoteArgumentDefinition remoteArgumentDefinition = underlyingServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getCompletedValue();

        String syntheticFieldName = underlyingServiceHydration.getSyntheticField();
        StringValue stringValue = StringValue.newStringValue(value.toString())
                .additionalData(NodeId.ID, UUID.randomUUID().toString())
                .build();

        Argument argument = Argument.newArgument()
                .name(remoteArgumentDefinition.getName())
                .value(stringValue)
                .additionalData(NodeId.ID, UUID.randomUUID().toString())
                .build();

        // set fieldID to the hydrationInputNode's ID so that later we can get the correct ObjectType for the RootResultNode in applySyntheticFieldChange
        return newField(syntheticFieldName)
                .selectionSet(originalField.getSelectionSet())
                .arguments(singletonList(argument))
                .additionalData(NodeId.ID, hydrationInputNode.getFieldIds().get(0))
                .build();
    }



    private Field createSingleHydrationTopLevelField(HydrationInputNode hydrationInputNode, SelectionSet selectionSet, UnderlyingServiceHydration underlyingServiceHydration, String topLevelFieldName, boolean isSynthetic) {
        if (isSynthetic) {
            return newField(topLevelFieldName)
                    .selectionSet(selectionSet)
                    .additionalData(NodeId.ID, UUID.randomUUID().toString())
                    .build();
        }
        RemoteArgumentDefinition remoteArgumentDefinition = underlyingServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getCompletedValue();
        Argument argument = Argument.newArgument()
                .name(remoteArgumentDefinition.getName())
                .value(new StringValue(value.toString()))
                .build();

        return newField(topLevelFieldName)
                .selectionSet(selectionSet)
                .arguments(singletonList(argument))
                .additionalData(NodeId.ID, UUID.randomUUID().toString())
                .build();
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
        ExecutionResultNode firstTopLevelResultNode = serviceResultNodesToOverallResult
                .convertChildren(executionId,
                        rootResultNode.getChildren().get(0),
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

        Field topLevelField;
        GraphQLCompositeType topLevelFieldType;

        boolean isSyntheticHydration = !underlyingServiceHydration.getSyntheticField().isEmpty();
        if (isSyntheticHydration) {
            Field syntheticField = createBatchSyntheticField(executionContext, originalField, underlyingServiceHydration, hydrationInputs);
            SelectionSet syntheticSelectionSet = newSelectionSet().selection(syntheticField).build();

            topLevelField = createBatchHydrationTopLevelField(executionContext, hydrationInputs, syntheticSelectionSet, null,  underlyingServiceHydration, true);
            topLevelFieldType = topLevelfieldTypeForSyntheticHydration(underlyingServiceHydration.getTopLevelField(), underlyingServiceHydration.getSyntheticField(), service);
        } else {
            topLevelField = createBatchHydrationTopLevelField(executionContext, hydrationInputs, originalField.getSelectionSet(), originalField.getArguments(), underlyingServiceHydration, false);
            topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());
        }

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

        QueryTransformationResult queryTransformationResult = queryTransformer
                .transformHydratedTopLevelField(executionContext, service.getUnderlyingSchema(), operationName, operation, topLevelField, topLevelFieldType, serviceExecutionHooks, service, serviceContexts.get(service));


        return serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation, serviceContexts.get(service), true)
                .thenApply(resultNode -> isSyntheticHydration ? applySyntheticFieldChange(resultNode, getNadelContext(executionContext), underlyingServiceHydration.getSyntheticField(), true) : resultNode)
                .thenApply(resultNode -> convertHydrationBatchResultIntoOverallResult(executionContext, hydrationInputs, resultNode, queryTransformationResult, resultComplexityAggregator))
                .whenComplete(this::possiblyLogException);

    }


    private RootExecutionResultNode applySyntheticFieldChange(RootExecutionResultNode resultNode, NadelContext nadelContext, String syntheticField, boolean isBatch) {

        ExecutionResultNode topLevelFieldNode = resultNode.getChildren().get(0);
        ExecutionResultNode syntheticResultNode = topLevelFieldNode.getChildren().stream().filter(child -> child.getFieldName().equals(syntheticField)).findFirst().orElseGet(null);
        assertNotNull(syntheticResultNode, () -> String.format("Synthetic field %s must not be empty.", syntheticField));

        String graphQLObjectTypeId = syntheticResultNode.getFieldIds().get(0);
        GraphQLObjectType newObjectType = nadelContext.getNormalizedOverallQuery().getNormalizedFieldsByFieldId(graphQLObjectTypeId).get(0).getObjectType();

        ExecutionResultNode newTopLevelResultNode;
        if (isBatch) {
            newTopLevelResultNode = ListExecutionResultNode.newListExecutionResultNode()
                    .objectType(newObjectType)
                    .executionPath(syntheticResultNode.getExecutionPath())
                    .children(syntheticResultNode.getChildren())
                    .fieldDefinition(syntheticResultNode.getFieldDefinition())
                    .fieldIds(syntheticResultNode.getFieldIds())
                    .elapsedTime(syntheticResultNode.getElapsedTime())
                    .completedValue(syntheticResultNode.getCompletedValue())
                    .alias(syntheticResultNode.getAlias())
                    .build();
        } else {
            newTopLevelResultNode = ObjectExecutionResultNode.newObjectExecutionResultNode()
                    .objectType(newObjectType)
                    .executionPath(syntheticResultNode.getExecutionPath())
                    .children(syntheticResultNode.getChildren())
                    .fieldDefinition(syntheticResultNode.getFieldDefinition())
                    .fieldIds(syntheticResultNode.getFieldIds())
                    .elapsedTime(syntheticResultNode.getElapsedTime())
                    .completedValue(syntheticResultNode.getCompletedValue())
                    .alias(syntheticResultNode.getAlias())
                    .build();
        }

        return resultNode.transform(builder -> builder.children(Arrays.asList(newTopLevelResultNode)));
    }

    private Field createBatchSyntheticField(ExecutionContext executionContext, Field originalField, UnderlyingServiceHydration underlyingServiceHydration, List<HydrationInputNode> hydrationInputs) {
        String syntheticFieldName = underlyingServiceHydration.getSyntheticField();

        List<RemoteArgumentDefinition> arguments = underlyingServiceHydration.getArguments();
        RemoteArgumentDefinition argumentFromSourceObject = findOneOrNull(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.OBJECT_FIELD);
        List<RemoteArgumentDefinition> extraArguments = filter(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.FIELD_ARGUMENT);

        List<Value> values = new ArrayList<>();
        for (ExecutionResultNode hydrationInputNode : hydrationInputs) {
            Object value = hydrationInputNode.getCompletedValue();
            values.add(StringValue.newStringValue(value.toString())
                    .additionalData(NodeId.ID, UUID.randomUUID().toString())
                    .build());
        }

        Argument argumentAstFromSourceObject = Argument.newArgument()
                .name(argumentFromSourceObject.getName())
                .value(ArrayValue.newArrayValue()
                        .values(values)
                        .additionalData(NodeId.ID, UUID.randomUUID().toString())
                        .build())
                .additionalData(NodeId.ID, UUID.randomUUID().toString())
                .build();
        List<Argument> allArguments = new ArrayList<>();
        allArguments.add(argumentAstFromSourceObject);

        Map<String, Argument> originalArgumentsByName = FpKit.getByName(originalField.getArguments(), Argument::getName);
        for (RemoteArgumentDefinition argumentDefinition : extraArguments) {
            if (originalArgumentsByName.containsKey(argumentDefinition.getName())) {
                allArguments.add(originalArgumentsByName.get(argumentDefinition.getName()));
            }
        }

        Field topLevelField = newField(syntheticFieldName)
                .selectionSet(originalField.getSelectionSet())
                .additionalData(NodeId.ID, hydrationInputs.get(0).getFieldIds().get(0))
                .arguments(allArguments)
                .build();
        return addObjectIdentifier(getNadelContext(executionContext), topLevelField, underlyingServiceHydration.getObjectIdentifier());
    }

    private Field createBatchHydrationTopLevelField(ExecutionContext executionContext,
                                                    List<HydrationInputNode> hydrationInputs,
                                                    SelectionSet selectionSet,
                                                    List<Argument> originalArguments,
                                                    UnderlyingServiceHydration underlyingServiceHydration,
                                                    boolean isSynthethic) {

        if (isSynthethic) {
            return newField(underlyingServiceHydration.getTopLevelField())
                    .selectionSet(selectionSet)
                    .additionalData(NodeId.ID, UUID.randomUUID().toString())
                    .build();
        }
        String topLevelFieldName = underlyingServiceHydration.getTopLevelField();
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

        Map<String, Argument> originalArgumentsByName = FpKit.getByName(originalArguments, Argument::getName);
        for (RemoteArgumentDefinition argumentDefinition : extraArguments) {
            if (originalArgumentsByName.containsKey(argumentDefinition.getName())) {
                allArguments.add(originalArgumentsByName.get(argumentDefinition.getName()));
            }
        }

        Field topLevelField = newField(topLevelFieldName)
                .selectionSet(selectionSet)
                .additionalData(NodeId.ID, UUID.randomUUID().toString())
                .arguments(allArguments)
                .build();
        return addObjectIdentifier(getNadelContext(executionContext), topLevelField, underlyingServiceHydration.getObjectIdentifier());
    }


    private List<ExecutionResultNode> convertHydrationBatchResultIntoOverallResult(ExecutionContext executionContext,
                                                                                   List<HydrationInputNode> hydrationInputNodes,
                                                                                   RootExecutionResultNode rootResultNode,
                                                                                   QueryTransformationResult queryTransformationResult,
                                                                                   ResultComplexityAggregator resultComplexityAggregator) {


        if (rootResultNode.getChildren().get(0) instanceof LeafExecutionResultNode) {
            // we only expect a null value here
            assertTrue(rootResultNode.getChildren().get(0).isNullValue());
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
        assertTrue(rootResultNode.getChildren().get(0) instanceof ListExecutionResultNode, () -> "expect a list result from the underlying service for batched hydration");
        ListExecutionResultNode listResultNode = (ListExecutionResultNode) rootResultNode.getChildren().get(0);
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
