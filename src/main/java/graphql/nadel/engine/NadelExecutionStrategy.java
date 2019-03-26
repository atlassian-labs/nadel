package graphql.nadel.engine;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.Internal;
import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.MergedField;
import graphql.execution.nextgen.ExecutionStrategy;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.StringValue;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionParameters;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
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
import java.util.concurrent.CompletionStage;

import static graphql.Assert.assertNotEmpty;
import static graphql.Assert.assertNotNull;
import static graphql.language.Field.newField;
import static graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters;
import static graphql.nadel.engine.FixListNamesAdapter.FIX_NAMES_ADAPTER;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.StrategyUtil.createRootExecutionStepInfo;
import static graphql.nadel.engine.StrategyUtil.getHydrationInputNodes;
import static graphql.nadel.engine.StrategyUtil.getHydrationTransformations;
import static graphql.util.FpKit.map;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

@Internal
public class NadelExecutionStrategy implements ExecutionStrategy {

    private final Logger log = LoggerFactory.getLogger(ExecutionStrategy.class);

    private final ServiceResultToResultNodes resultToResultNode = new ServiceResultToResultNodes();
    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult();
    private final OverallQueryTransformer queryTransformer = new OverallQueryTransformer();

    private final List<Service> services;
    private final FieldInfos fieldInfos;
    private final GraphQLSchema overallSchema;

    public NadelExecutionStrategy(List<Service> services, FieldInfos fieldInfos, GraphQLSchema overallSchema) {
        this.overallSchema = overallSchema;
        assertNotEmpty(services);
        this.services = services;
        this.fieldInfos = fieldInfos;
    }

    @Override
    public CompletableFuture<RootExecutionResultNode> execute(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        Map<Service, List<MergedField>> delegatedExecutionForTopLevel = getDelegatedExecutionForTopLevel(context, fieldSubSelection);

        String operationName = context.getOperationDefinition().getName();
        Operation operation = Operation.fromAst(context.getOperationDefinition().getOperation());

        List<CompletableFuture<RootExecutionResultNode>> resultNodes = new ArrayList<>();
        List<HydrationTransformation> hydrationTransformations = new ArrayList<>();
        for (Service service : delegatedExecutionForTopLevel.keySet()) {
            List<MergedField> mergedFields = delegatedExecutionForTopLevel.get(service);
            QueryTransformationResult queryTransformerResult = queryTransformer.transformMergedFields(context, mergedFields, operation, operationName);
            hydrationTransformations.addAll(getHydrationTransformations(queryTransformerResult.getTransformationByResultField().values()));
            resultNodes.add(callService(context, queryTransformerResult, service, operation));
        }


        CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);

        return rootResult.thenCompose(rootExecutionResultNode -> resolveAllHydrationInputs(context, rootExecutionResultNode, hydrationTransformations).
                thenApply(RootExecutionResultNode.class::cast))
                .whenComplete(this::possiblyLogException);
    }


    private CompletableFuture<ExecutionResultNode> resolveAllHydrationInputs(ExecutionContext context,
                                                                             ExecutionResultNode node,
                                                                             List<HydrationTransformation> hydrationTransformations) {
        List<NodeZipper<ExecutionResultNode>> hydrationInputZippers = getHydrationInputNodes(singleton(node));


        List<CompletableFuture<NodeZipper<ExecutionResultNode>>> resolvedNodeCFs = new ArrayList<>();

        for (NodeZipper<ExecutionResultNode> zipper : hydrationInputZippers) {
            Field field = zipper.getCurNode().getMergedField().getSingleField();
            HydrationTransformation transformationForField = getTransformationForField(hydrationTransformations, field);
            resolvedNodeCFs.add(resolveHydrationInput(context, zipper.getCurNode(), transformationForField).thenApply(zipper::withNewNode));
        }
        return Async
                .each(resolvedNodeCFs)
                .thenApply(resolvedNodes -> {
                    NodeMultiZipper<ExecutionResultNode> multiZipper = new NodeMultiZipper<>(node, resolvedNodes, FIX_NAMES_ADAPTER);
                    return multiZipper.toRootNode();
                })
                .whenComplete(this::possiblyLogException);

    }

    @SuppressWarnings("unused")
    private <T> void possiblyLogException(T result, Throwable exception) {
        if (exception != null) {
            exception.printStackTrace();
        }
    }


    private HydrationTransformation getTransformationForField(List<HydrationTransformation> transformations, Field field) {
        return transformations.stream().filter(transformation -> transformation.getNewField() == field).findFirst().get();
    }

    private CompletableFuture<RootExecutionResultNode> callService(ExecutionContext executionContext,
                                                                   QueryTransformationResult queryTransformerResult,
                                                                   Service service,
                                                                   Operation operation) {

        Map<Field, FieldTransformation> transformationByResultField = queryTransformerResult.getTransformationByResultField();
        List<MergedField> transformedMergedFields = queryTransformerResult.getTransformedMergedFields();

        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, queryTransformerResult);
        ExecutionContext executionContextForService = buildServiceExecutionContext(executionContext, underlyingSchema, serviceExecutionParameters);

        ExecutionStepInfo rootExecutionStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema(), operation);

        CompletableFuture<ServiceExecutionResult> result = invokeService(service, serviceExecution, serviceExecutionParameters, rootExecutionStepInfo);
        assertNotNull(result, "A service execution MUST provide a non null CompletableFuture<ServiceExecutionResult> ");
        return result
                .thenApply(executionResult -> resultToResultNode(executionContextForService, rootExecutionStepInfo, transformedMergedFields, executionResult))
                .thenApply(resultNode -> serviceResultNodesToOverallResult.convert(resultNode, overallSchema, transformationByResultField));
    }


    private CompletableFuture<ExecutionResultNode> resolveHydrationInput(ExecutionContext executionContext,
                                                                         ExecutionResultNode hydrationInputNode,
                                                                         HydrationTransformation hydrationTransformation) {
        Field originalField = hydrationTransformation.getOriginalField();
        InnerServiceHydration innerServiceHydration = hydrationTransformation.getInnerServiceHydration();
        String topLevelFieldName = innerServiceHydration.getTopLevelField();

        // TODO: just assume String arguments at the moment
        RemoteArgumentDefinition remoteArgumentDefinition = innerServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getFetchedValueAnalysis().getCompletedValue();
        Argument argument = Argument.newArgument().name(remoteArgumentDefinition.getName()).value(new StringValue(value.toString())).build();

        Field topLevelField = newField(topLevelFieldName).selectionSet(originalField.getSelectionSet())
                .arguments(singletonList(argument))
                .build();

        Service service = getService(innerServiceHydration);

        QueryTransformationResult queryTransformResult = queryTransformer.transformSelectionSetInField(executionContext,
                topLevelField,
                (GraphQLOutputType) GraphQLTypeUtil.unwrapAll(hydrationTransformation.getFieldType()));

        MergedField transformedMergedField = MergedField.newMergedField(queryTransformResult.getTransformedField()).build();

        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
        Operation operation = Operation.fromAst(executionContext.getOperationDefinition().getOperation());

        ExecutionStepInfo rootExecutionStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema(), operation);
        Map<Field, FieldTransformation> transformationByResultField = queryTransformResult.getTransformationByResultField();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, queryTransformResult);
        ExecutionContext executionContextForService = buildServiceExecutionContext(executionContext, underlyingSchema, serviceExecutionParameters);

        CompletableFuture<ServiceExecutionResult> result = invokeService(service, serviceExecution, serviceExecutionParameters, rootExecutionStepInfo);
        assertNotNull(result, "A service execution MUST provide a non null CompletableFuture<ServiceExecutionResult> ");
        return result
                .thenApply(executionResult -> resultToResultNode(executionContextForService, rootExecutionStepInfo, singletonList(transformedMergedField), executionResult))
                .thenApply(resultNode -> convertHydrationResultIntoOverallResult(hydrationTransformation, resultNode, transformationByResultField))
                .thenCompose(resultNode -> runHydrationTransformations(executionContextForService, transformationByResultField, resultNode))
                .whenComplete(this::possiblyLogException);
    }

    private RootExecutionResultNode resultToResultNode(ExecutionContext executionContextForService, ExecutionStepInfo rootExecutionStepInfo, List<MergedField> transformedMergedFields, ServiceExecutionResult executionResult) {
        return resultToResultNode.resultToResultNode(executionContextForService, rootExecutionStepInfo, transformedMergedFields, executionResult);
    }

    private CompletionStage<ExecutionResultNode> runHydrationTransformations(ExecutionContext executionContext, Map<Field, FieldTransformation> transformationByResultField, ExecutionResultNode resultNode) {
        List<HydrationTransformation> hydrationTransformations = getHydrationTransformations(transformationByResultField.values());
        return resolveAllHydrationInputs(executionContext, resultNode, hydrationTransformations);
    }

    private ExecutionResultNode convertHydrationResultIntoOverallResult(HydrationTransformation hydrationTransformation,
                                                                        RootExecutionResultNode rootResultNode,
                                                                        Map<Field, FieldTransformation> transformationByResultField) {
        RootExecutionResultNode overallResultNode = serviceResultNodesToOverallResult.convert(rootResultNode, overallSchema, transformationByResultField);
        // NOTE : we only take the first result node here but we may have errors in the root node that are global so transfer them in
        ExecutionResultNode firstTopLevelResultNode = overallResultNode.getChildren().get(0);
        firstTopLevelResultNode = firstTopLevelResultNode.withNewErrors(rootResultNode.getErrors());
        return changeFieldInResultNode(firstTopLevelResultNode, hydrationTransformation.getOriginalField());
    }


    private Service getService(InnerServiceHydration innerServiceHydration) {
        return FpKit.findOne(services, service -> service.getName().equals(innerServiceHydration.getServiceName())).get();
    }

    private CompletableFuture<ServiceExecutionResult> invokeService(Service service, ServiceExecution serviceExecution, ServiceExecutionParameters serviceExecutionParameters, ExecutionStepInfo executionStepInfo) {
        try {
            log.debug("service {} invocation started", service.getName());
            CompletableFuture<ServiceExecutionResult> result = serviceExecution.execute(serviceExecutionParameters);
            log.debug("service {} invocation finished ", service.getName());
            return result;
        } catch (Exception e) {
            String errorText = String.format("An exception occurred invoking the service '%s' : '%s'", service.getName(), e.getMessage());
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

            Map<String, Object> dataMap = new LinkedHashMap<>();

            ServiceExecutionResult result = new ServiceExecutionResult(dataMap, singletonList(errorMap));
            return completedFuture(result);
        }
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

    private Map<Service, List<MergedField>> getDelegatedExecutionForTopLevel(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        //TODO: consider dynamic delegation targets in the future
        Map<Service, List<MergedField>> result = new LinkedHashMap<>();
        ExecutionStepInfo executionStepInfo = fieldSubSelection.getExecutionStepInfo();
        for (MergedField mergedField : fieldSubSelection.getMergedSelectionSet().getSubFieldsList()) {
            ExecutionStepInfo newExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(context, mergedField, executionStepInfo);
            Service service = getServiceForFieldDefinition(newExecutionStepInfo.getFieldDefinition());
            result.computeIfAbsent(service, key -> new ArrayList<>());
            result.get(service).add(mergedField);
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

        return newServiceExecutionParameters()
                .query(queryTransformerResult.getDocument())
                .context(executionContext.getContext())
                .variables(variables)
                .fragments(fragments)
                .operationDefinition(executionContext.getOperationDefinition())
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

}
