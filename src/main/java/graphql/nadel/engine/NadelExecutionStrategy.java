package graphql.nadel.engine;

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
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.StringValue;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionParameters;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static graphql.Assert.assertNotEmpty;
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

@Internal
public class NadelExecutionStrategy implements ExecutionStrategy {

    private final ServiceResultToResultNodes resultToResultNode = new ServiceResultToResultNodes();
    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult();
    private final OverallQueryTransformer queryTransformer = new OverallQueryTransformer();

    private final List<Service> services;
    private FieldInfos fieldInfos;
    private GraphQLSchema overallSchema;

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
            resultNodes.add(callService(context, queryTransformerResult, service, fieldSubSelection.getExecutionStepInfo(), operation));
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

    private <T> void possiblyLogException(T result, Throwable exception) {
        if (exception != null) {
            exception.printStackTrace();
        }
    }


    private HydrationTransformation getTransformationForField(List<HydrationTransformation> transformations, Field field) {
        return transformations.stream().filter(transformation -> transformation.getNewField() == field).findFirst().get();
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
        Document document = queryTransformResult.getDocument();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, document);

        ExecutionStepInfo rootExecutionStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema(), operation);
        Map<Field, FieldTransformation> transformationByResultField = queryTransformResult.getTransformationByResultField();
        return serviceExecution.execute(serviceExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(executionContext, delegatedExecutionResult, rootExecutionStepInfo, singletonList(transformedMergedField), underlyingSchema, Operation.QUERY))
                .thenApply(resultNode -> convertHydrationResultIntoOverallResult(hydrationTransformation, resultNode, transformationByResultField))
                .thenCompose(resultNode -> runHydrationTransformations(executionContext, transformationByResultField, resultNode))
                .whenComplete(this::possiblyLogException);
    }

    private CompletionStage<ExecutionResultNode> runHydrationTransformations(ExecutionContext executionContext, Map<Field, FieldTransformation> transformationByResultField, ExecutionResultNode resultNode) {
        List<HydrationTransformation> hydrationTransformations = getHydrationTransformations(transformationByResultField.values());
        return resolveAllHydrationInputs(executionContext, resultNode, hydrationTransformations);
    }

    private ExecutionResultNode convertHydrationResultIntoOverallResult(HydrationTransformation hydrationTransformation,
                                                                        RootExecutionResultNode resultNode,
                                                                        Map<Field, FieldTransformation> transformationByResultField) {
        RootExecutionResultNode overallResultNode = serviceResultNodesToOverallResult.convert(resultNode, overallSchema, transformationByResultField);
        ExecutionResultNode topLevelResultNode = overallResultNode.getChildren().get(0);
        return changeFieldInResultNode(topLevelResultNode, hydrationTransformation.getOriginalField());
    }


    private Service getService(InnerServiceHydration innerServiceHydration) {
        return FpKit.findOne(services, service -> service.getName().equals(innerServiceHydration.getServiceName())).get();
    }


    private CompletableFuture<RootExecutionResultNode> mergeTrees(List<CompletableFuture<RootExecutionResultNode>> resultNodes) {
        return Async.each(resultNodes).thenApply(rootNodes -> {
            List<ExecutionResultNode> mergedChildren = new ArrayList<>();
            map(rootNodes, ExecutionResultNode::getChildren).forEach(mergedChildren::addAll);
            return new RootExecutionResultNode(mergedChildren);
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

    private CompletableFuture<RootExecutionResultNode> callService(ExecutionContext executionContext,
                                                                   QueryTransformationResult queryTransformerResult,
                                                                   Service service,
                                                                   ExecutionStepInfo rootExecutionStepInfo,
                                                                   Operation operation) {

        Document query = queryTransformerResult.getDocument();
        Map<Field, FieldTransformation> transformationByResultField = queryTransformerResult.getTransformationByResultField();
        List<MergedField> transformedMergedFields = queryTransformerResult.getTransformedMergedFields();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, query);
        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        return serviceExecution.execute(serviceExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(executionContext, delegatedExecutionResult, rootExecutionStepInfo, transformedMergedFields, underlyingSchema, operation))
                .thenApply(resultNode -> serviceResultNodesToOverallResult.convert(resultNode, overallSchema, transformationByResultField));
    }

    private ServiceExecutionParameters buildServiceExecutionParameters(ExecutionContext executionContext, Document query) {
        return newServiceExecutionParameters()
                    .query(query)
                    .context(executionContext.getContext())
                    .variables(executionContext.getVariables())
                    .fragments(executionContext.getFragmentsByName())
                    .operationDefinition(executionContext.getOperationDefinition())
                    .build();
    }
}
