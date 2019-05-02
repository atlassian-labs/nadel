package graphql.nadel.engine;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
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
import graphql.language.FragmentDefinition;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionParameters;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.engine.tracking.FieldTracking;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationServiceExecutionParameters;
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
import java.util.function.BiFunction;

import static graphql.Assert.assertNotEmpty;
import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.language.Field.newField;
import static graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters;
import static graphql.nadel.engine.FixListNamesAdapter.FIX_NAMES_ADAPTER;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.StrategyUtil.createRootExecutionStepInfo;
import static graphql.nadel.engine.StrategyUtil.getHydrationInputNodes;
import static graphql.nadel.engine.StrategyUtil.groupNodesIntoBatchesByField;
import static graphql.nadel.engine.UnderscoreTypeNameUtils.maybeRemoveUnderscoreTypeName;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.schema.GraphQLTypeUtil.unwrapNonNull;
import static graphql.util.FpKit.findOneOrNull;
import static graphql.util.FpKit.flatList;
import static graphql.util.FpKit.map;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
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

    public NadelExecutionStrategy(List<Service> services, FieldInfos fieldInfos, GraphQLSchema overallSchema, NadelInstrumentation instrumentation) {
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
        assertNotEmpty(services);
        this.services = services;
        this.fieldInfos = fieldInfos;
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
            CompletableFuture<RootExecutionResultNode> serviceResult = callService(executionContext, rootExecutionStepInfo, queryTransformerResult, service, operation);

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
                                .thenApply(resultNode -> maybeRemoveUnderscoreTypeName(getNadelContext(executionContext), resultNode))
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

        // TODO: just assume String arguments at the moment
        RemoteArgumentDefinition remoteArgumentDefinition = innerServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getFetchedValueAnalysis().getCompletedValue();
        Argument argument = Argument.newArgument()
                .name(remoteArgumentDefinition.getName())
                .value(new StringValue(value.toString()))
                .build();

        Field topLevelField = newField(topLevelFieldName)
                .selectionSet(originalField.getSelectionSet())
                .arguments(singletonList(argument))
                .build();

        Service service = getService(innerServiceHydration);

        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());
        QueryTransformationResult queryTransformResult = queryTransformer
                .transformHydratedTopLevelField(executionContext, operationName, operation, topLevelField, topLevelFieldType);

        MergedField transformedMergedField = MergedField.newMergedField(queryTransformResult.getTransformedField()).build();

        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        ExecutionStepInfo underlyingRootStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema(), operation);
        Map<Field, FieldTransformation> transformationByResultField = queryTransformResult.getTransformationByResultField();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, queryTransformResult);
        ExecutionContext executionContextForService = buildServiceExecutionContext(executionContext, underlyingSchema, serviceExecutionParameters);

        CompletableFuture<ServiceExecutionResult> callResult = invokeService(service, serviceExecution, serviceExecutionParameters, underlyingRootStepInfo, executionContext);
        assertNotNull(callResult, "A service execution MUST provide a non null CompletableFuture<ServiceExecutionResult> ");

        //
        // tell the fields tracking we are dispatched
        fieldTracking.fieldsDispatched(singletonList(hydratedFieldStepInfo));
        return callResult
                .thenApply(serviceResult -> serviceExecutionResultToResultNode(executionContextForService, underlyingRootStepInfo, singletonList(transformedMergedField), serviceResult))
                .thenApply(resultNode -> convertSingleHydrationResultIntoOverallResult(fieldTracking, hydratedFieldStepInfo, hydrationTransformation, resultNode, transformationByResultField))
                .thenApply(resultNode -> maybeRemoveUnderscoreTypeName(getNadelContext(executionContext), resultNode))
                .whenComplete(fieldTracking::fieldsCompleted)
                .thenCompose(resultNode -> resolveAllHydrationInputs(executionContextForService, fieldTracking, resultNode))
                .whenComplete(this::possiblyLogException);

    }

    private ExecutionResultNode convertSingleHydrationResultIntoOverallResult(FieldTracking fieldTracking,
                                                                              ExecutionStepInfo hydratedFieldStepInfo,
                                                                              HydrationTransformation hydrationTransformation,
                                                                              RootExecutionResultNode rootResultNode,
                                                                              Map<Field, FieldTransformation> transformationByResultField) {

        synthesizeHydratedParentIfNeeded(fieldTracking, hydratedFieldStepInfo);

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

        String topLevelFieldName = innerServiceHydration.getTopLevelField();

        // TODO: just assume String arguments at the moment
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


        Operation operation = Operation.QUERY;
        String operationName = buildOperationName(service, executionContext);

        GraphQLCompositeType topLevelFieldType = (GraphQLCompositeType) unwrapAll(hydrationTransformation.getOriginalFieldType());
        QueryTransformationResult queryTransformResult = queryTransformer
                .transformHydratedTopLevelField(executionContext, operationName, operation, topLevelField, topLevelFieldType);

        MergedField transformedMergedField = MergedField.newMergedField(queryTransformResult.getTransformedField()).build();

        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        ExecutionStepInfo underlyingRootStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema(), operation);
        Map<Field, FieldTransformation> transformationByResultField = queryTransformResult.getTransformationByResultField();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, queryTransformResult);
        ExecutionContext executionContextForService = buildServiceExecutionContext(executionContext, underlyingSchema, serviceExecutionParameters);

        CompletableFuture<ServiceExecutionResult> callResult = invokeService(service, serviceExecution, serviceExecutionParameters, underlyingRootStepInfo, executionContext);
        assertNotNull(callResult, "A service execution MUST provide a non null CompletableFuture<ServiceExecutionResult> ");

        List<ExecutionStepInfo> hydratedFieldStepInfos = map(hydrationInputs, hydrationInputNode -> hydrationInputNode.getFetchedValueAnalysis().getExecutionStepInfo());
        fieldTracking.fieldsDispatched(hydratedFieldStepInfos);
        return callResult
                .thenApply(serviceResult -> serviceExecutionResultToResultNode(executionContextForService, underlyingRootStepInfo, singletonList(transformedMergedField), serviceResult))
                .thenApply(resultNode -> convertHydrationBatchResultIntoOverallResult(fieldTracking, hydrationInputs, resultNode, transformationByResultField))
                .thenApply(resultNode -> maybeRemoveUnderscoreTypeName(getNadelContext(executionContext), resultNode))
                .whenComplete(fieldTracking::fieldsCompleted)
                .whenComplete(this::possiblyLogException);

    }

    private RootExecutionResultNode serviceExecutionResultToResultNode(ExecutionContext executionContextForService, ExecutionStepInfo underlyingRootStepInfo, List<MergedField> transformedMergedFields, ServiceExecutionResult executionResult) {
        return resultToResultNode.resultToResultNode(executionContextForService, underlyingRootStepInfo, transformedMergedFields, executionResult);
    }

    private List<ExecutionResultNode> convertHydrationBatchResultIntoOverallResult(FieldTracking fieldTracking,
                                                                                   List<HydrationInputNode> hydrationInputNodes,
                                                                                   RootExecutionResultNode rootResultNode,
                                                                                   Map<Field, FieldTransformation> transformationByResultField) {

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

        boolean first = true;
        for (HydrationInputNode hydrationInputNode : hydrationInputNodes) {
            ExecutionStepInfo executionStepInfo = hydrationInputNode.getFetchedValueAnalysis().getExecutionStepInfo();
            ObjectExecutionResultNode matchingResolvedNode = findMatchingResolvedNode(hydrationInputNode, resolvedNodes);
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

    private ObjectExecutionResultNode findMatchingResolvedNode(HydrationInputNode inputNode, List<ExecutionResultNode> resolvedNodes) {
        String inputNodeId = (String) inputNode.getFetchedValueAnalysis().getCompletedValue();
        for (ExecutionResultNode resolvedNode : resolvedNodes) {
            LeafExecutionResultNode idNode = getFieldValue((ObjectExecutionResultNode) resolvedNode, "id");
            String id = (String) idNode.getFetchedValueAnalysis().getCompletedValue();
            if (id.equals(inputNodeId)) {
                return (ObjectExecutionResultNode) resolvedNode;
            }
        }
        return null;
    }

    private LeafExecutionResultNode getFieldValue(ObjectExecutionResultNode node, String fieldName) {
        return (LeafExecutionResultNode) findOneOrNull(node.getChildren(), child -> child.getMergedField().getResultKey().equals(fieldName));
    }

    private List<ExecutionResultNode> handleNullHydrationResult(HydrationTransformation hydrationTransformation, RootExecutionResultNode rootResultNode, List<ExecutionResultNode> hydrationInputs, RootExecutionResultNode overallResultNode) {
        List<ExecutionResultNode> result = new ArrayList<>();
        boolean first = true;
        // we need the same list length
        for (ExecutionResultNode hydrationInputNode : hydrationInputs) {
            ExecutionResultNode nullNode = overallResultNode.getChildren().get(0);
            if (first) {
                nullNode = nullNode.withNewErrors(rootResultNode.getErrors());
                first = false;
            }
            result.add(changeFieldInResultNode(nullNode, hydrationTransformation.getOriginalField()));
        }
        return result;
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

    private CompletableFuture<RootExecutionResultNode> callService(ExecutionContext executionContext,
                                                                   ExecutionStepInfo rootExecutionStepInfo, QueryTransformationResult queryTransformerResult,
                                                                   Service service,
                                                                   Operation operation) {

        Map<Field, FieldTransformation> transformationByResultField = queryTransformerResult.getTransformationByResultField();
        List<MergedField> transformedMergedFields = queryTransformerResult.getTransformedMergedFields();

        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, queryTransformerResult);
        ExecutionContext executionContextForService = buildServiceExecutionContext(executionContext, underlyingSchema, serviceExecutionParameters);

        ExecutionStepInfo underlyingRootStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema(), operation);

        CompletableFuture<ServiceExecutionResult> result = invokeService(service, serviceExecution, serviceExecutionParameters, underlyingRootStepInfo, executionContext);
        assertNotNull(result, "A service execution MUST provide a non null CompletableFuture<ServiceExecutionResult> ");
        return result
                .thenApply(executionResult -> serviceExecutionResultToResultNode(executionContextForService, underlyingRootStepInfo, transformedMergedFields, executionResult))
                .thenApply(resultNode -> (RootExecutionResultNode) serviceResultNodesToOverallResult.convert(resultNode, overallSchema, rootExecutionStepInfo, transformationByResultField));
    }

    private CompletableFuture<ServiceExecutionResult> invokeService(Service service, ServiceExecution serviceExecution, ServiceExecutionParameters serviceExecutionParameters, ExecutionStepInfo executionStepInfo, ExecutionContext executionContext) {

        NadelInstrumentationServiceExecutionParameters instrumentationParams = new NadelInstrumentationServiceExecutionParameters(service, executionContext, executionContext.getInstrumentationState());
        serviceExecution = instrumentation.instrumentServiceExecution(serviceExecution, instrumentationParams);

        try {
            log.debug("service {} invocation started", service.getName());
            CompletableFuture<ServiceExecutionResult> result = serviceExecution.execute(serviceExecutionParameters);
            log.debug("service {} invocation finished ", service.getName());
            //
            // if they return an exceptional CF then we turn that into graphql errors as well
            return result.handle(handleServiceException(service, executionStepInfo));
        } catch (Exception e) {
            return completedFuture(mkExceptionResult(service, executionStepInfo, e));
        }
    }

    private BiFunction<ServiceExecutionResult, Throwable, ServiceExecutionResult> handleServiceException(Service service, ExecutionStepInfo executionStepInfo) {
        return (serviceCallResult, throwable) -> {
            if (throwable != null) {
                return mkExceptionResult(service, executionStepInfo, throwable);
            } else {
                return serviceCallResult;
            }
        };
    }

    private ServiceExecutionResult mkExceptionResult(Service service, ExecutionStepInfo executionStepInfo, Throwable e) {
        String errorText = format("An exception occurred invoking the service '%s' : '%s'", service.getName(), e.getMessage());
        log.error(errorText, e);

        GraphqlErrorBuilder errorBuilder = GraphqlErrorBuilder.newError();
        MergedField field = executionStepInfo.getField();
        if (field != null) {
            errorBuilder.location(field.getSingleField().getSourceLocation());
        }
        GraphQLError error = errorBuilder
                .message(errorText)
                .path(executionStepInfo.getPath())
                .errorType(ErrorType.DataFetchingException)
                .build();

        Map<String, Object> errorMap = error.toSpecification();
        return new ServiceExecutionResult(new LinkedHashMap<>(), singletonList(errorMap));
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

    private ServiceExecutionParameters buildServiceExecutionParameters(ExecutionContext executionContext, QueryTransformationResult queryTransformerResult) {

        // only pass down variables that are referenced in the transformed query
        Map<String, Object> contextVariables = executionContext.getVariables();
        Map<String, Object> variables = new LinkedHashMap<>();
        for (String referencedVariable : queryTransformerResult.getReferencedVariables()) {
            Object value = contextVariables.get(referencedVariable);
            variables.put(referencedVariable, value);
        }

        // only pass down fragments that have been used by the query after it is transformed
        Map<String, FragmentDefinition> fragments = queryTransformerResult.getTransformedFragments();

        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        Object callerSuppliedContext = nadelContext.getUserSuppliedContext();

        return newServiceExecutionParameters()
                .query(queryTransformerResult.getDocument())
                .context(callerSuppliedContext)
                .variables(variables)
                .fragments(fragments)
                .operationDefinition(queryTransformerResult.getOperationDefinition())
                .executionId(executionContext.getExecutionId())
                .cacheControl(executionContext.getCacheControl())
                .build();
    }

    private ExecutionContext buildServiceExecutionContext(ExecutionContext executionContext, GraphQLSchema underlyingSchema, ServiceExecutionParameters serviceExecutionParameters) {
        return executionContext.transform(builder -> builder
                .graphQLSchema(underlyingSchema)
                .fragmentsByName(serviceExecutionParameters.getFragments())
                .variables(serviceExecutionParameters.getVariables())
                .operationDefinition(serviceExecutionParameters.getOperationDefinition())
        );
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


