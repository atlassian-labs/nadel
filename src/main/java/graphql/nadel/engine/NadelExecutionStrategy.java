package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.MergedField;
import graphql.execution.nextgen.ExecutionStrategy;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.ResultNodeTraverser;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.StringValue;
import graphql.nadel.DelegatedExecutionParameters;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecution;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.util.FpKit;
import graphql.util.NodeZipper;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotEmpty;
import static graphql.Assert.assertTrue;
import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo;
import static graphql.execution.nextgen.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;
import static graphql.language.Document.newDocument;
import static graphql.language.Field.newField;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.DelegatedExecutionParameters.newDelegatedExecutionParameters;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@Internal
public class NadelExecutionStrategy implements ExecutionStrategy {

    private final ServiceResultToResultNodes resultToResultNode = new ServiceResultToResultNodes();
    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult();

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

        List<CompletableFuture<RootExecutionResultNode>> resultNodes = new ArrayList<>();
        List<HydrationTransformation> hydrationTransformations = new ArrayList<>();
        List<OverallQueryTransformer> queryTransformers = new ArrayList<>();
        for (Service service : delegatedExecutionForTopLevel.keySet()) {
            List<MergedField> mergedFields = delegatedExecutionForTopLevel.get(service);
            OverallQueryTransformer queryTransformer = transformQuery(context, mergedFields);
            hydrationTransformations.addAll(getHydrationTransformations(queryTransformer.getTransformationByResultField().values()));
            queryTransformers.add(queryTransformer);
            resultNodes.add(callService(context, queryTransformer, mergedFields, service, fieldSubSelection.getExecutionStepInfo()));
        }


        CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);

        return rootResult.thenApply(rootExecutionResultNode -> {
            List<NodeZipper<ExecutionResultNode>> hydrationInputs = getHydrationInputNodes(singleton(rootExecutionResultNode));
            Map<Field, List<NodeZipper<ExecutionResultNode>>> nodesByField = FpKit.groupingBy(hydrationInputs, hydrationZipper -> hydrationZipper.getCurNode().getMergedField().getSingleField());

            for (Field field : nodesByField.keySet()) {
                List<NodeZipper<ExecutionResultNode>> nodeZippers = nodesByField.get(field);
                HydrationTransformation transformationForField = getTransformationForField(hydrationTransformations, field);
                makeHydrationCall(context, nodeZippers, transformationForField);
            }
            return rootExecutionResultNode;
        });
    }

    private HydrationTransformation getTransformationForField(List<HydrationTransformation> transformations, Field field) {
        return transformations.stream().filter(transformation -> transformation.getNewField() == field).findFirst().get();
    }

    private List<HydrationTransformation> getHydrationTransformations(Collection<FieldTransformation> transformations) {
        return transformations
                .stream()
                .filter(transformation -> transformation instanceof HydrationTransformation)
                .map(HydrationTransformation.class::cast)
                .collect(Collectors.toList());
    }

    private CompletableFuture<RootExecutionResultNode> makeHydrationCall(ExecutionContext context,
                                                                         List<NodeZipper<ExecutionResultNode>> hydrationInputs,
                                                                         HydrationTransformation hydrationTransformation) {
        //TODO: we only support one hydration input at the moment
        assertTrue(hydrationInputs.size() == 1, "only single hydration inputs at the moment");
        HydrationInputNode hydrationInputNode = (HydrationInputNode) hydrationInputs.get(0).getCurNode();
        Field originalField = hydrationTransformation.getOriginalField();
        InnerServiceHydration innerServiceHydration = hydrationTransformation.getInnerServiceHydration();
        String topLevelFieldName = innerServiceHydration.getTopLevelField();

        // TODO: just assume String arguments at the moment
        RemoteArgumentDefinition remoteArgumentDefinition = innerServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getFetchedValueAnalysis().getFetchedValue().getFetchedValue();
        Argument argument = Argument.newArgument().name(remoteArgumentDefinition.getName()).value(new StringValue(value.toString())).build();

        Field topLevelField = newField(topLevelFieldName).selectionSet(originalField.getSelectionSet())
                .arguments(singletonList(argument))
                .build();
        OperationDefinition operationDefinition = newOperationDefinition().operation(OperationDefinition.Operation.QUERY)
                .selectionSet(newSelectionSet().selection(topLevelField).build())
                .build();
        Document newDocument = newDocument()
                .definition(operationDefinition)
                .build();

        Service service = getService(innerServiceHydration);
        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters()
                .query(newDocument)
                .build();

        MergedField mergedField = MergedField.newMergedField(topLevelField).build();

        ExecutionStepInfo rootExecutionStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema());
        return serviceExecution.execute(delegatedExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(context, delegatedExecutionResult, rootExecutionStepInfo, singletonList(mergedField), underlyingSchema))
                .thenApply(resultNode -> {
                    RootExecutionResultNode result = serviceResultNodesToOverallResult.convert(resultNode, overallSchema, emptyMap());
                    return result;
                });

    }

    private ExecutionStepInfo createRootExecutionStepInfo(GraphQLSchema graphQLSchema) {
        ExecutionStepInfo executionInfo = newExecutionStepInfo().type(graphQLSchema.getQueryType()).path(ExecutionPath.rootPath()).build();
        return executionInfo;
    }

    private Service getService(InnerServiceHydration innerServiceHydration) {
        return FpKit.findOne(services, service -> service.getName().equals(innerServiceHydration.getServiceName())).get();
    }


    private CompletableFuture<RootExecutionResultNode> mergeTrees(List<CompletableFuture<RootExecutionResultNode>> resultNodes) {
        return Async.each(resultNodes).thenApply(rootNodes -> {
            Map<String, ExecutionResultNode> mergedChildren = new LinkedHashMap<>();
            rootNodes.forEach(rootNode -> mergedChildren.putAll(rootNode.getChildrenMap()));
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

    private CompletableFuture<RootExecutionResultNode> callService(ExecutionContext context,
                                                                   OverallQueryTransformer queryTransformer,
                                                                   List<MergedField> mergedFields,
                                                                   Service service,
                                                                   ExecutionStepInfo rootExecutionStepInfo) {

        Document query = queryTransformer.delegateDocument();
        Map<Field, FieldTransformation> transformationByResultField = queryTransformer.getTransformationByResultField();
        List<MergedField> transformedMergedFields = queryTransformer.getTransformedMergedFields();

        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters()
                .query(query)
                .build();
        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        return serviceExecution.execute(delegatedExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(context, delegatedExecutionResult, rootExecutionStepInfo, transformedMergedFields, underlyingSchema))
                .thenApply(resultNode -> serviceResultNodesToOverallResult.convert(resultNode, overallSchema, transformationByResultField));

    }

    private OverallQueryTransformer transformQuery(ExecutionContext context, List<MergedField> mergedFields) {
        OverallQueryTransformer queryTransformer = new OverallQueryTransformer(context);
        queryTransformer.transform(mergedFields, OperationDefinition.Operation.QUERY);
        return queryTransformer;
    }

    public static List<NodeZipper<ExecutionResultNode>> getHydrationInputNodes(Collection<ExecutionResultNode> roots) {
        List<NodeZipper<ExecutionResultNode>> result = new ArrayList<>();

        ResultNodeTraverser traverser = ResultNodeTraverser.depthFirst();
        traverser.traverse(new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof HydrationInputNode) {
                    result.add(new NodeZipper<>(context.thisNode(), context.getBreadcrumbs(), RESULT_NODE_ADAPTER));
                }
                return TraversalControl.CONTINUE;
            }

        }, roots);
        return result;
    }

}
