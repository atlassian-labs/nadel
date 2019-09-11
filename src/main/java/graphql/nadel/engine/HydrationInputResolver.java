package graphql.nadel.engine;

import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.Field;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.dsl.UnderlyingServiceHydration;
import graphql.nadel.engine.tracking.FieldTracking;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.util.ExecutionPathUtils;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.util.FpKit;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.language.Field.newField;
import static graphql.nadel.engine.ArtificialFieldUtils.addObjectIdentifier;
import static graphql.nadel.engine.ArtificialFieldUtils.removeArtificialFields;
import static graphql.nadel.engine.FixListNamesAdapter.FIX_NAMES_ADAPTER;
import static graphql.nadel.engine.StrategyUtil.changeEsiInResultNode;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.StrategyUtil.getHydrationInputNodes;
import static graphql.nadel.engine.StrategyUtil.groupNodesIntoBatchesByField;
import static graphql.nadel.util.FpKit.filter;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.schema.GraphQLTypeUtil.unwrapNonNull;
import static graphql.util.FpKit.findOneOrNull;
import static graphql.util.FpKit.flatList;
import static graphql.util.FpKit.map;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

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
                                                                            FieldTracking fieldTracking,
                                                                            ExecutionResultNode node) {
        NadelContext nadelContext = (NadelContext) context.getContext();
        Set<NodeZipper<ExecutionResultNode>> hydrationInputZippers = getHydrationInputNodes(nadelContext.getForkJoinPool(), node);
        if (hydrationInputZippers.size() == 0) {
            return CompletableFuture.completedFuture(node);
        }

        List<NodeMultiZipper<ExecutionResultNode>> hydrationInputBatches = groupNodesIntoBatchesByField(hydrationInputZippers, node);

        List<CompletableFuture<List<NodeZipper<ExecutionResultNode>>>> resolvedNodeCFs = new ArrayList<>();

        for (NodeMultiZipper<ExecutionResultNode> batch : hydrationInputBatches) {
            if (isBatchHydrationField((HydrationInputNode) batch.getZippers().get(0).getCurNode())) {
                resolveInputNodesAsBatch(context, fieldTracking, resolvedNodeCFs, batch);
            } else {
                resolveInputNodes(context, fieldTracking, resolvedNodeCFs, batch);
            }

        }
        return Async
                .each(resolvedNodeCFs)
                .thenCompose(resolvedNodes -> {
                    NodeMultiZipper<ExecutionResultNode> multiZipper = new NodeMultiZipper<>(node, flatList(resolvedNodes), FIX_NAMES_ADAPTER);
                    ExecutionResultNode newRoot = multiZipper.toRootNode();
                    return resolveAllHydrationInputs(context, fieldTracking, newRoot);
                })
                .whenComplete(this::possiblyLogException);
    }

    private void resolveInputNodes(ExecutionContext context, FieldTracking fieldTracking, List<CompletableFuture<List<NodeZipper<ExecutionResultNode>>>> resolvedNodeCFs, NodeMultiZipper<ExecutionResultNode> batch) {
        for (NodeZipper<ExecutionResultNode> hydrationInputNodeZipper : batch.getZippers()) {
            HydrationInputNode hydrationInputNode = (HydrationInputNode) hydrationInputNodeZipper.getCurNode();
            CompletableFuture<ExecutionResultNode> executionResultNodeCompletableFuture = resolveSingleHydrationInput(context, fieldTracking, hydrationInputNode);
            resolvedNodeCFs.add(executionResultNodeCompletableFuture.thenApply(newNode -> singletonList(hydrationInputNodeZipper.withNewNode(newNode))));
        }
    }

    private void resolveInputNodesAsBatch(ExecutionContext context, FieldTracking fieldTracking, List<CompletableFuture<List<NodeZipper<ExecutionResultNode>>>> resolvedNodeCFs, NodeMultiZipper<ExecutionResultNode> batch) {
        List<NodeMultiZipper<ExecutionResultNode>> batchesWithCorrectSize = groupIntoCorrectBatchSizes(batch);
        for (NodeMultiZipper<ExecutionResultNode> oneBatch : batchesWithCorrectSize) {
            List<HydrationInputNode> batchedNodes = map(oneBatch.getZippers(), zipper -> (HydrationInputNode) zipper.getCurNode());
            CompletableFuture<List<ExecutionResultNode>> executionResultNodeCompletableFuture = resolveHydrationInputBatch(context, fieldTracking, batchedNodes);
            resolvedNodeCFs.add(replaceNodesInZipper(oneBatch, executionResultNodeCompletableFuture));
        }
    }

    private List<NodeMultiZipper<ExecutionResultNode>> groupIntoCorrectBatchSizes(NodeMultiZipper<ExecutionResultNode> batch) {
        HydrationInputNode node = (HydrationInputNode) batch.getZippers().get(0).getCurNode();
        Integer batchSize = node.getHydrationTransformation().getUnderlyingServiceHydration().getBatchSize();
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
                result.add(new NodeMultiZipper<>(batch.getCommonRoot(), currentBatch, FIX_NAMES_ADAPTER));
                counter = 0;
                currentBatch = new ArrayList<>();
            }
        }
        if (currentBatch.size() > 0) {
            result.add(new NodeMultiZipper<>(batch.getCommonRoot(), currentBatch, FIX_NAMES_ADAPTER));
        }
        return result;
    }


    private boolean isBatchHydrationField(HydrationInputNode hydrationInputNode) {
        HydrationTransformation hydrationTransformation = hydrationInputNode.getHydrationTransformation();
        Service service = getService(hydrationTransformation.getUnderlyingServiceHydration());
        String topLevelFieldName = hydrationTransformation.getUnderlyingServiceHydration().getTopLevelField();
        GraphQLFieldDefinition topLevelFieldDefinition = service.getUnderlyingSchema().getQueryType().getFieldDefinition(topLevelFieldName);
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
                                                                               FieldTracking fieldTracking,
                                                                               HydrationInputNode hydrationInputNode) {
        HydrationTransformation hydrationTransformation = hydrationInputNode.getHydrationTransformation();
        ExecutionStepInfo hydratedFieldStepInfo = hydrationInputNode.getExecutionStepInfo();

        Field originalField = hydrationTransformation.getOriginalField();
        UnderlyingServiceHydration underlyingServiceHydration = hydrationTransformation.getUnderlyingServiceHydration();
        String topLevelFieldName = underlyingServiceHydration.getTopLevelField();

        Field topLevelField = createSingleHydrationTopLevelField(hydrationInputNode, originalField, underlyingServiceHydration, topLevelFieldName);

        Service service = getService(underlyingServiceHydration);

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);
        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());

        QueryTransformationResult queryTransformationResult = queryTransformer
                .transformHydratedTopLevelField(executionContext, service.getUnderlyingSchema(), operationName, operation, topLevelField, topLevelFieldType, serviceExecutionHooks);


        fieldTracking.fieldsDispatched(singletonList(hydratedFieldStepInfo));

        CompletableFuture<RootExecutionResultNode> serviceResult = serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation, null);

        ForkJoinPool forkJoinPool = getNadelContext(executionContext).getForkJoinPool();
        return serviceResult
                .thenApply(resultNode -> removeArtificialFieldsFromRoot(executionContext, resultNode))
                .thenApply(resultNode -> convertSingleHydrationResultIntoOverallResult(executionContext.getExecutionId(), forkJoinPool, fieldTracking, hydratedFieldStepInfo, hydrationTransformation, resultNode, queryTransformationResult))
                .whenComplete(fieldTracking::fieldsCompleted)
                .whenComplete(this::possiblyLogException);

    }


    private Field createSingleHydrationTopLevelField(HydrationInputNode hydrationInputNode, Field originalField, UnderlyingServiceHydration underlyingServiceHydration, String topLevelFieldName) {
        RemoteArgumentDefinition remoteArgumentDefinition = underlyingServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getResolvedValue().getCompletedValue();
        Argument argument = Argument.newArgument()
                .name(remoteArgumentDefinition.getName())
                .value(new StringValue(value.toString()))
                .build();

        return newField(topLevelFieldName)
                .selectionSet(originalField.getSelectionSet())
                .arguments(singletonList(argument))
                .build();
    }

    private ExecutionResultNode convertSingleHydrationResultIntoOverallResult(ExecutionId executionId,
                                                                              ForkJoinPool forkJoinPool,
                                                                              FieldTracking fieldTracking,
                                                                              ExecutionStepInfo hydratedFieldStepInfo,
                                                                              HydrationTransformation hydrationTransformation,
                                                                              RootExecutionResultNode rootResultNode,
                                                                              QueryTransformationResult queryTransformationResult) {

        synthesizeHydratedParentIfNeeded(fieldTracking, hydratedFieldStepInfo);

        Map<String, FieldTransformation> transformationByResultField = queryTransformationResult.getTransformationByResultField();
        Map<String, String> typeRenameMappings = queryTransformationResult.getTypeRenameMappings();
        ExecutionResultNode firstTopLevelResultNode = serviceResultNodesToOverallResult
                .convertChildren(executionId, forkJoinPool, rootResultNode.getChildren().get(0), overallSchema, hydratedFieldStepInfo, true, false, transformationByResultField, typeRenameMappings);
        firstTopLevelResultNode = firstTopLevelResultNode.withNewErrors(rootResultNode.getErrors());
        firstTopLevelResultNode = changeEsiInResultNode(firstTopLevelResultNode, hydratedFieldStepInfo);

        return changeFieldInResultNode(firstTopLevelResultNode, hydrationTransformation.getOriginalField());
    }

    private CompletableFuture<List<ExecutionResultNode>> resolveHydrationInputBatch(ExecutionContext executionContext,
                                                                                    FieldTracking fieldTracking,
                                                                                    List<HydrationInputNode> hydrationInputs) {

        List<HydrationTransformation> hydrationTransformations = map(hydrationInputs, HydrationInputNode::getHydrationTransformation);


        HydrationTransformation hydrationTransformation = hydrationTransformations.get(0);
        Field originalField = hydrationTransformation.getOriginalField();
        UnderlyingServiceHydration underlyingServiceHydration = hydrationTransformation.getUnderlyingServiceHydration();
        Service service = getService(underlyingServiceHydration);

        Field topLevelField = createBatchHydrationTopLevelField(executionContext, hydrationInputs, originalField, underlyingServiceHydration);

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());
        QueryTransformationResult queryTransformationResult = queryTransformer
                .transformHydratedTopLevelField(executionContext, service.getUnderlyingSchema(), operationName, operation, topLevelField, topLevelFieldType, serviceExecutionHooks);


        List<ExecutionStepInfo> hydratedFieldStepInfos = map(hydrationInputs, ExecutionResultNode::getExecutionStepInfo);
        fieldTracking.fieldsDispatched(hydratedFieldStepInfos);
        return serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation, null)
                .thenApply(resultNode -> convertHydrationBatchResultIntoOverallResult(executionContext, fieldTracking, hydrationInputs, resultNode, queryTransformationResult))
                .thenApply(resultNode -> ArtificialFieldUtils.removeArtificialFields(getNadelContext(executionContext), resultNode))
                .whenComplete(fieldTracking::fieldsCompleted)
                .whenComplete(this::possiblyLogException);

    }

    private Field createBatchHydrationTopLevelField(ExecutionContext executionContext,
                                                    List<HydrationInputNode> hydrationInputs,
                                                    Field originalField,
                                                    UnderlyingServiceHydration underlyingServiceHydration) {
        String topLevelFieldName = underlyingServiceHydration.getTopLevelField();
        List<RemoteArgumentDefinition> arguments = underlyingServiceHydration.getArguments();
        RemoteArgumentDefinition argumentFromSourceObject = findOneOrNull(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.OBJECT_FIELD);
        List<RemoteArgumentDefinition> extraArguments = filter(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.FIELD_ARGUMENT);

        List<Value> values = new ArrayList<>();
        for (ExecutionResultNode hydrationInputNode : hydrationInputs) {
            Object value = hydrationInputNode.getResolvedValue().getCompletedValue();
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
                .arguments(allArguments)
                .build();
        return addObjectIdentifier(getNadelContext(executionContext), topLevelField, underlyingServiceHydration.getObjectIdentifier());
    }


    private List<ExecutionResultNode> convertHydrationBatchResultIntoOverallResult(ExecutionContext executionContext,
                                                                                   FieldTracking fieldTracking,
                                                                                   List<HydrationInputNode> hydrationInputNodes,
                                                                                   RootExecutionResultNode rootResultNode,
                                                                                   QueryTransformationResult queryTransformationResult) {

        List<ExecutionStepInfo> hydratedFieldStepInfos = map(hydrationInputNodes, ExecutionResultNode::getExecutionStepInfo);
        synthesizeHydratedParentIfNeeded(fieldTracking, hydratedFieldStepInfos);

        if (rootResultNode.getChildren().get(0) instanceof LeafExecutionResultNode) {
            // we only expect a null value here
            assertTrue(rootResultNode.getChildren().get(0).getResolvedValue().isNullValue());
            List<ExecutionResultNode> result = new ArrayList<>();
            for (HydrationInputNode hydrationInputNode : hydrationInputNodes) {
                ExecutionStepInfo executionStepInfo = hydrationInputNode.getExecutionStepInfo();
                ExecutionResultNode resultNode = createNullValue(executionStepInfo);
                result.add(resultNode);
            }
            return result;
        }
        assertTrue(rootResultNode.getChildren().get(0) instanceof ListExecutionResultNode, "expect a list result from the underlying service for batched hydration");
        ListExecutionResultNode listResultNode = (ListExecutionResultNode) rootResultNode.getChildren().get(0);
        List<ExecutionResultNode> resolvedNodes = listResultNode.getChildren();

        List<ExecutionResultNode> result = new ArrayList<>();
        Map<String, FieldTransformation> transformationByResultField = queryTransformationResult.getTransformationByResultField();
        Map<String, String> typeRenameMappings = queryTransformationResult.getTypeRenameMappings();

        boolean first = true;
        ForkJoinPool forkJoinPool = getNadelContext(executionContext).getForkJoinPool();
        for (HydrationInputNode hydrationInputNode : hydrationInputNodes) {
            ExecutionStepInfo executionStepInfo = hydrationInputNode.getExecutionStepInfo();
            ObjectExecutionResultNode matchingResolvedNode = findMatchingResolvedNode(executionContext, hydrationInputNode, resolvedNodes);
            ExecutionResultNode resultNode;
            if (matchingResolvedNode != null) {
                ExecutionResultNode overallResultNode = serviceResultNodesToOverallResult.convertChildren(
                        executionContext.getExecutionId(),
                        forkJoinPool,
                        matchingResolvedNode,
                        overallSchema,
                        executionStepInfo,
                        true,
                        true,
                        transformationByResultField,
                        typeRenameMappings);
                Field originalField = hydrationInputNode.getHydrationTransformation().getOriginalField();
                resultNode = changeFieldInResultNode(overallResultNode, originalField);
            } else {
                resultNode = createNullValue(executionStepInfo);
            }
            if (first) {
                resultNode = resultNode.withNewErrors(rootResultNode.getErrors());
                first = false;
            }
            result.add(resultNode);
        }
        return result;

    }

    private LeafExecutionResultNode createNullValue(ExecutionStepInfo executionStepInfo) {
        ResolvedValue resolvedValue = ResolvedValue.newResolvedValue().completedValue(null)
                .localContext(null)
                .nullValue(true)
                .build();
        return new LeafExecutionResultNode(executionStepInfo, resolvedValue, null);
    }

    private ObjectExecutionResultNode findMatchingResolvedNode(ExecutionContext executionContext, HydrationInputNode inputNode, List<ExecutionResultNode> resolvedNodes) {
        NadelContext nadelContext = getNadelContext(executionContext);
        String objectIdentifier = nadelContext.getObjectIdentifierAlias();
        String inputNodeId = (String) inputNode.getResolvedValue().getCompletedValue();
        for (ExecutionResultNode resolvedNode : resolvedNodes) {
            LeafExecutionResultNode idNode = getFieldByResultKey((ObjectExecutionResultNode) resolvedNode, objectIdentifier);
            assertNotNull(idNode, "no value found for object identifier: " + objectIdentifier);
            Object id = idNode.getResolvedValue().getCompletedValue();
            assertNotNull(id, "object identifier is null");
            if (id.equals(inputNodeId)) {
                return (ObjectExecutionResultNode) resolvedNode;
            }
        }
        return null;
    }

    private RootExecutionResultNode removeArtificialFieldsFromRoot(ExecutionContext executionContext, RootExecutionResultNode root) {
        return (RootExecutionResultNode) removeArtificialFields(getNadelContext(executionContext), root);
    }

    private LeafExecutionResultNode getFieldByResultKey(ObjectExecutionResultNode node, String resultKey) {
        return (LeafExecutionResultNode) findOneOrNull(node.getChildren(), child -> child.getMergedField().getResultKey().equals(resultKey));
    }


    private void synthesizeHydratedParentIfNeeded(FieldTracking fieldTracking, List<ExecutionStepInfo> hydratedFieldStepInfos) {
        for (ExecutionStepInfo hydratedFieldStepInfo : hydratedFieldStepInfos) {
            synthesizeHydratedParentIfNeeded(fieldTracking, hydratedFieldStepInfo);
        }
    }

    private void synthesizeHydratedParentIfNeeded(FieldTracking fieldTracking, ExecutionStepInfo hydratedFieldStepInfo) {
        ExecutionPath path = hydratedFieldStepInfo.getPath();
        if (ExecutionPathUtils.isListEndingPath(path)) {
            //
            // a path like /issues/comments[0] wont ever have had a /issues/comments path completed but the tracing spec needs
            // one so we make one
            ExecutionPath newPath = ExecutionPathUtils.removeLastSegment(path);
            ExecutionStepInfo newStepInfo = hydratedFieldStepInfo.transform(builder -> builder.path(newPath));
            fieldTracking.fieldCompleted(newStepInfo);
        }

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
