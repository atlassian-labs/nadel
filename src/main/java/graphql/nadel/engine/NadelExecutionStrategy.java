package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.FetchedValue;
import graphql.execution.MergedField;
import graphql.execution.nextgen.ExecutionStrategy;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.Field;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.engine.tracking.FieldTracking;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.util.ExecutionPathUtils;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.util.FpKit;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotEmpty;
import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.language.Field.newField;
import static graphql.nadel.engine.ArtificialFieldUtils.addObjectIdentifier;
import static graphql.nadel.engine.ArtificialFieldUtils.removeArtificialFields;
import static graphql.nadel.engine.FixListNamesAdapter.FIX_NAMES_ADAPTER;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.StrategyUtil.getHydrationInputNodes;
import static graphql.nadel.engine.StrategyUtil.groupNodesIntoBatchesByField;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.schema.GraphQLTypeUtil.unwrapNonNull;
import static graphql.util.FpKit.findOneOrNull;
import static graphql.util.FpKit.flatList;
import static graphql.util.FpKit.map;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Internal
public class NadelExecutionStrategy {

    private final Logger log = LoggerFactory.getLogger(ExecutionStrategy.class);

    private final ServiceResultToResultNodes resultToResultNode = new ServiceResultToResultNodes();
    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult();
    private final OverallQueryTransformer queryTransformer = new OverallQueryTransformer();

    private final List<Service> services;
    private final FieldInfos fieldInfos;
    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final ServiceExecutor serviceExecutor;

    public NadelExecutionStrategy(List<Service> services, FieldInfos fieldInfos, GraphQLSchema overallSchema, NadelInstrumentation instrumentation) {
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
        assertNotEmpty(services);
        this.services = services;
        this.fieldInfos = fieldInfos;
        this.serviceExecutor = new ServiceExecutor(overallSchema, instrumentation);
    }

    public CompletableFuture<RootExecutionResultNode> execute(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        ExecutionStepInfo rootExecutionStepInfo = fieldSubSelection.getExecutionStepInfo();

        Map<Service, List<ExecutionStepInfo>> delegatedExecutionForTopLevel = getDelegatedExecutionForTopLevel(executionContext, fieldSubSelection, rootExecutionStepInfo);

        FieldTracking fieldTracking = new FieldTracking(instrumentation, executionContext);

        Operation operation = Operation.fromAst(executionContext.getOperationDefinition().getOperation());

        List<CompletableFuture<RootExecutionResultNode>> resultNodes = new ArrayList<>();
        for (Service service : delegatedExecutionForTopLevel.keySet()) {
            String operationName = buildOperationName(service, executionContext);
            List<ExecutionStepInfo> stepInfos = delegatedExecutionForTopLevel.get(service);
            List<MergedField> mergedFields = stepInfos.stream().map(ExecutionStepInfo::getField).collect(toList());

            //
            // take the original query and transform it into the underlying query needed for that top level field
            //
            QueryTransformationResult queryTransformerResult = queryTransformer.transformMergedFields(executionContext, operationName, operation, mergedFields);

            //
            // say they are dispatched
            fieldTracking.fieldsDispatched(stepInfos);
            //
            // now call put to the service with the new query
            CompletableFuture<RootExecutionResultNode> serviceResult = serviceExecutor
                    .execute(executionContext, queryTransformerResult, service, operation)
                    .thenApply(resultNode -> (RootExecutionResultNode) serviceResultNodesToOverallResult.convert(resultNode, overallSchema, rootExecutionStepInfo, queryTransformerResult.getTransformationByResultField()));

            //
            // and then they are done call back on field tracking that they have completed (modulo hydrated ones).  This is per service call
            serviceResult.whenComplete(fieldTracking::fieldsCompleted);

            resultNodes.add(serviceResult);
        }

        CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);
        return rootResult
                .thenCompose(
                        //
                        // all the nodes that are hydrated need to make new service calls to get their eventual value
                        //
                        rootExecutionResultNode -> resolveAllHydrationInputs(executionContext, fieldTracking, rootExecutionResultNode)
                                //
                                // clean up the __typename support for interfaces
                                .thenApply(resultNode -> removeArtificialFields(getNadelContext(executionContext), resultNode))
                                .thenApply(RootExecutionResultNode.class::cast))
                .whenComplete(this::possiblyLogException);
    }


    private CompletableFuture<ExecutionResultNode> resolveAllHydrationInputs(ExecutionContext context,
                                                                             FieldTracking fieldTracking,
                                                                             ExecutionResultNode node) {
        List<NodeZipper<ExecutionResultNode>> hydrationInputZippers = getHydrationInputNodes(singleton(node));
        if (hydrationInputZippers.size() == 0) {
            return CompletableFuture.completedFuture(node);
        }

        List<NodeMultiZipper<ExecutionResultNode>> hydrationInputBatches = groupNodesIntoBatchesByField(hydrationInputZippers, node);

        List<CompletableFuture<List<NodeZipper<ExecutionResultNode>>>> resolvedNodeCFs = new ArrayList<>();

        for (NodeMultiZipper<ExecutionResultNode> batch : hydrationInputBatches) {

            List<HydrationInputNode> batchedNodes = map(batch.getZippers(), zipper -> (HydrationInputNode) zipper.getCurNode());

            if (isBatchHydrationField(batchedNodes.get(0))) {
                CompletableFuture<List<ExecutionResultNode>> executionResultNodeCompletableFuture = resolveHydrationInputBatch(context, fieldTracking, batchedNodes);
                resolvedNodeCFs.add(replaceNodesInZipper(batch, executionResultNodeCompletableFuture));
            } else {
                for (NodeZipper<ExecutionResultNode> hydrationInputNodeZipper : batch.getZippers()) {
                    HydrationInputNode hydrationInputNode = (HydrationInputNode) hydrationInputNodeZipper.getCurNode();
                    CompletableFuture<ExecutionResultNode> executionResultNodeCompletableFuture = resolveSingleHydrationInput(context, fieldTracking, hydrationInputNode);
                    resolvedNodeCFs.add(executionResultNodeCompletableFuture.thenApply(newNode -> singletonList(hydrationInputNodeZipper.withNewNode(newNode))));
                }
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

    private boolean isBatchHydrationField(HydrationInputNode hydrationInputNode) {
        HydrationTransformation hydrationTransformation = hydrationInputNode.getHydrationTransformation();
        Service service = getService(hydrationTransformation.getInnerServiceHydration());
        String topLevelFieldName = hydrationTransformation.getInnerServiceHydration().getTopLevelField();
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
        ExecutionStepInfo hydratedFieldStepInfo = hydrationInputNode.getFetchedValueAnalysis().getExecutionStepInfo();

        Field originalField = hydrationTransformation.getOriginalField();
        InnerServiceHydration innerServiceHydration = hydrationTransformation.getInnerServiceHydration();
        String topLevelFieldName = innerServiceHydration.getTopLevelField();

        Field topLevelField = createSingleHydrationTopLevelField(hydrationInputNode, originalField, innerServiceHydration, topLevelFieldName);

        Service service = getService(innerServiceHydration);

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);
        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());

        QueryTransformationResult queryTransformationResult = queryTransformer
                .transformHydratedTopLevelField(executionContext, operationName, operation, topLevelField, topLevelFieldType);


        fieldTracking.fieldsDispatched(singletonList(hydratedFieldStepInfo));
        return serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation)
                .thenApply(resultNode -> convertSingleHydrationResultIntoOverallResult(fieldTracking, hydratedFieldStepInfo, hydrationTransformation, resultNode, queryTransformationResult))
                .thenApply(resultNode -> removeArtificialFields(getNadelContext(executionContext), resultNode))
                .whenComplete(fieldTracking::fieldsCompleted)
                .whenComplete(this::possiblyLogException);

    }

    private Field createSingleHydrationTopLevelField(HydrationInputNode hydrationInputNode, Field originalField, InnerServiceHydration innerServiceHydration, String topLevelFieldName) {
        RemoteArgumentDefinition remoteArgumentDefinition = innerServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getFetchedValueAnalysis().getCompletedValue();
        Argument argument = Argument.newArgument()
                .name(remoteArgumentDefinition.getName())
                .value(new StringValue(value.toString()))
                .build();

        return newField(topLevelFieldName)
                .selectionSet(originalField.getSelectionSet())
                .arguments(singletonList(argument))
                .build();
    }

    private ExecutionResultNode convertSingleHydrationResultIntoOverallResult(FieldTracking fieldTracking,
                                                                              ExecutionStepInfo hydratedFieldStepInfo,
                                                                              HydrationTransformation hydrationTransformation,
                                                                              RootExecutionResultNode rootResultNode,
                                                                              QueryTransformationResult queryTransformationResult) {

        synthesizeHydratedParentIfNeeded(fieldTracking, hydratedFieldStepInfo);

        Map<Field, FieldTransformation> transformationByResultField = queryTransformationResult.getTransformationByResultField();
        RootExecutionResultNode overallResultNode = (RootExecutionResultNode) serviceResultNodesToOverallResult.convert(rootResultNode, overallSchema, hydratedFieldStepInfo, true, false, transformationByResultField);
        // NOTE : we only take the first result node here but we may have errors in the root node that are global so transfer them in
        ExecutionResultNode firstTopLevelResultNode = overallResultNode.getChildren().get(0);
        firstTopLevelResultNode = firstTopLevelResultNode.withNewErrors(rootResultNode.getErrors());

        return changeFieldInResultNode(firstTopLevelResultNode, hydrationTransformation.getOriginalField());
    }

    private CompletableFuture<List<ExecutionResultNode>> resolveHydrationInputBatch(ExecutionContext executionContext,
                                                                                    FieldTracking fieldTracking,
                                                                                    List<HydrationInputNode> hydrationInputs) {

        List<HydrationTransformation> hydrationTransformations = map(hydrationInputs, HydrationInputNode::getHydrationTransformation);


        HydrationTransformation hydrationTransformation = hydrationTransformations.get(0);
        Field originalField = hydrationTransformation.getOriginalField();
        InnerServiceHydration innerServiceHydration = hydrationTransformation.getInnerServiceHydration();
        Service service = getService(innerServiceHydration);

        Field topLevelField = createBatchHydrationTopLevelField(executionContext, hydrationInputs, originalField, innerServiceHydration);

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());
        QueryTransformationResult queryTransformationResult = queryTransformer
                .transformHydratedTopLevelField(executionContext, operationName, operation, topLevelField, topLevelFieldType);


        List<ExecutionStepInfo> hydratedFieldStepInfos = map(hydrationInputs, hydrationInputNode -> hydrationInputNode.getFetchedValueAnalysis().getExecutionStepInfo());
        fieldTracking.fieldsDispatched(hydratedFieldStepInfos);
        return serviceExecutor
                .execute(executionContext, queryTransformationResult, service, operation)
                .thenApply(resultNode -> convertHydrationBatchResultIntoOverallResult(executionContext, fieldTracking, hydrationInputs, resultNode, queryTransformationResult))
                .thenApply(resultNode -> ArtificialFieldUtils.removeArtificialFields(getNadelContext(executionContext), resultNode))
                .whenComplete(fieldTracking::fieldsCompleted)
                .whenComplete(this::possiblyLogException);

    }

    private Field createBatchHydrationTopLevelField(ExecutionContext executionContext, List<HydrationInputNode> hydrationInputs, Field originalField, InnerServiceHydration innerServiceHydration) {
        String topLevelFieldName = innerServiceHydration.getTopLevelField();
        RemoteArgumentDefinition remoteArgumentDefinition = innerServiceHydration.getArguments().get(0);
        List<Value> values = new ArrayList<>();
        for (ExecutionResultNode hydrationInputNode : hydrationInputs) {
            Object value = hydrationInputNode.getFetchedValueAnalysis().getCompletedValue();
            values.add(StringValue.newStringValue(value.toString()).build());
        }
        Argument argument = Argument.newArgument().name(remoteArgumentDefinition.getName()).value(new ArrayValue(values)).build();

        Field topLevelField = newField(topLevelFieldName)
                .selectionSet(originalField.getSelectionSet())
                .arguments(singletonList(argument))
                .build();
        return addObjectIdentifier(getNadelContext(executionContext), topLevelField, innerServiceHydration.getObjectIdentifier());
    }

    private List<ExecutionResultNode> convertHydrationBatchResultIntoOverallResult(ExecutionContext executionContext,
                                                                                   FieldTracking fieldTracking,
                                                                                   List<HydrationInputNode> hydrationInputNodes,
                                                                                   RootExecutionResultNode rootResultNode,
                                                                                   QueryTransformationResult queryTransformationResult) {

        List<ExecutionStepInfo> hydratedFieldStepInfos = map(hydrationInputNodes, hydrationInputNode -> hydrationInputNode.getFetchedValueAnalysis().getExecutionStepInfo());
        synthesizeHydratedParentIfNeeded(fieldTracking, hydratedFieldStepInfos);

        if (rootResultNode.getChildren() instanceof LeafExecutionResultNode) {
            // we only expect a null value here
            assertTrue(rootResultNode.getChildren().get(0).getFetchedValueAnalysis().isNullValue());
            throw new RuntimeException("null result from hydration call not implemented yet");
        }
        assertTrue(rootResultNode.getChildren().get(0) instanceof ListExecutionResultNode, "expect a list result from the underlying service for hydration");
        ListExecutionResultNode listResultNode = (ListExecutionResultNode) rootResultNode.getChildren().get(0);
        List<ExecutionResultNode> resolvedNodes = listResultNode.getChildren();

        List<ExecutionResultNode> result = new ArrayList<>();
        Map<Field, FieldTransformation> transformationByResultField = queryTransformationResult.getTransformationByResultField();

        boolean first = true;
        for (HydrationInputNode hydrationInputNode : hydrationInputNodes) {
            ExecutionStepInfo executionStepInfo = hydrationInputNode.getFetchedValueAnalysis().getExecutionStepInfo();
            ObjectExecutionResultNode matchingResolvedNode = findMatchingResolvedNode(executionContext, hydrationInputNode, resolvedNodes);
            ExecutionResultNode resultNode;
            if (matchingResolvedNode != null) {
                ExecutionResultNode overallResultNode = serviceResultNodesToOverallResult.convert(matchingResolvedNode, overallSchema, executionStepInfo, true, true, transformationByResultField);
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
        FetchedValueAnalysis fetchedValueAnalysis = FetchedValueAnalysis.newFetchedValueAnalysis()
                .valueType(FetchedValueAnalysis.FetchedValueType.OBJECT)
                .fetchedValue(FetchedValue.newFetchedValue().build())
                .nullValue()
                .executionStepInfo(executionStepInfo)
                .build();
        return new LeafExecutionResultNode(fetchedValueAnalysis, null);
    }

    private ObjectExecutionResultNode findMatchingResolvedNode(ExecutionContext executionContext, HydrationInputNode inputNode, List<ExecutionResultNode> resolvedNodes) {
        NadelContext nadelContext = getNadelContext(executionContext);
        String objectIdentifier = nadelContext.getObjectIdentifierAlias();
        String inputNodeId = (String) inputNode.getFetchedValueAnalysis().getCompletedValue();
        for (ExecutionResultNode resolvedNode : resolvedNodes) {
            LeafExecutionResultNode idNode = getFieldByResultKey((ObjectExecutionResultNode) resolvedNode, objectIdentifier);
            assertNotNull(idNode, "no value found for object identifier: " + objectIdentifier);
            String id = (String) idNode.getFetchedValueAnalysis().getCompletedValue();
            assertNotNull(id, "object identifier is null");
            if (id.equals(inputNodeId)) {
                return (ObjectExecutionResultNode) resolvedNode;
            }
        }
        return null;
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
    private Service getService(InnerServiceHydration innerServiceHydration) {
        return FpKit.findOne(services, service -> service.getName().equals(innerServiceHydration.getServiceName())).get();
    }

    private CompletableFuture<RootExecutionResultNode> mergeTrees(List<CompletableFuture<RootExecutionResultNode>> resultNodes) {
        return Async.each(resultNodes).thenApply(rootNodes -> {
            List<ExecutionResultNode> mergedChildren = new ArrayList<>();
            List<GraphQLError> errors = new ArrayList<>();
            map(rootNodes, RootExecutionResultNode::getChildren).forEach(mergedChildren::addAll);
            map(rootNodes, RootExecutionResultNode::getErrors).forEach(errors::addAll);
            return new RootExecutionResultNode(mergedChildren, errors);
        });
    }

    private Map<Service, List<ExecutionStepInfo>> getDelegatedExecutionForTopLevel(ExecutionContext context, FieldSubSelection fieldSubSelection, ExecutionStepInfo rootExecutionStepInfo) {
        //TODO: consider dynamic delegation targets in the future
        Map<Service, List<ExecutionStepInfo>> result = new LinkedHashMap<>();
        for (MergedField mergedField : fieldSubSelection.getMergedSelectionSet().getSubFieldsList()) {
            ExecutionStepInfo newExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(context, mergedField, rootExecutionStepInfo);
            Service service = getServiceForFieldDefinition(newExecutionStepInfo.getFieldDefinition());
            result.computeIfAbsent(service, key -> new ArrayList<>());
            result.get(service).add(newExecutionStepInfo);
        }
        return result;
    }

    private Service getServiceForFieldDefinition(GraphQLFieldDefinition fieldDefinition) {
        FieldInfo info = fieldInfos.getInfo(fieldDefinition);
        return info.getService();
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


