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
import graphql.execution.nextgen.result.NamedResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.ResultNodeTraverser;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.nadel.DelegatedExecutionParameters;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecution;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.FieldUtils;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.util.FpKit;
import graphql.util.NodeMultiZipper;
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
import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo;
import static graphql.execution.nextgen.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;
import static graphql.language.Field.newField;
import static graphql.nadel.DelegatedExecutionParameters.newDelegatedExecutionParameters;
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

        List<CompletableFuture<RootExecutionResultNode>> resultNodes = new ArrayList<>();
        List<HydrationTransformation> hydrationTransformations = new ArrayList<>();
        for (Service service : delegatedExecutionForTopLevel.keySet()) {
            List<MergedField> mergedFields = delegatedExecutionForTopLevel.get(service);
            OverallQueryTransformer.QueryTransformResult queryTransformerResult = queryTransformer.transform(context, mergedFields);
            hydrationTransformations.addAll(getHydrationTransformations(queryTransformerResult.transformationByResultField.values()));
            resultNodes.add(callService(context, queryTransformerResult, service, fieldSubSelection.getExecutionStepInfo()));
        }


        CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);

        return rootResult
                .thenCompose(rootExecutionResultNode -> {
                    List<NodeZipper<ExecutionResultNode>> hydrationInputs = getHydrationInputNodes(singleton(rootExecutionResultNode));
                    return executeHydration(context, hydrationInputs, hydrationTransformations)
                            .thenApply(nodeZippers -> {
                                NodeMultiZipper<ExecutionResultNode> multiZipper = new NodeMultiZipper<>(rootExecutionResultNode, nodeZippers, RESULT_NODE_ADAPTER);
                                ExecutionResultNode result = multiZipper.toRootNode();
                                return (RootExecutionResultNode) result;
                            });
                });
    }

    private static class HydrationInfo {

        public HydrationInfo(Field field, HydrationTransformation hydrationTransformation, NodeZipper<ExecutionResultNode> hydrationInputNodeZipper) {
            this.field = field;
            this.hydrationTransformation = hydrationTransformation;
            this.hydrationInputNodeZipper = hydrationInputNodeZipper;
        }

        Field field;
        HydrationTransformation hydrationTransformation;
        NodeZipper<ExecutionResultNode> hydrationInputNodeZipper;
    }

    private List<HydrationInfo> getHydrationInfos(List<NodeZipper<ExecutionResultNode>> hydrationInputsZippers, List<HydrationTransformation> hydrationTransformations) {
        Map<Field, List<NodeZipper<ExecutionResultNode>>> nodesByField = FpKit.groupingBy(hydrationInputsZippers, hydrationZipper -> hydrationZipper.getCurNode().getMergedField().getSingleField());

        List<HydrationInfo> result = new ArrayList<>();
        for (Field field : nodesByField.keySet()) {
            List<NodeZipper<ExecutionResultNode>> nodeZippers = nodesByField.get(field);
            HydrationTransformation transformationForField = getTransformationForField(hydrationTransformations, field);
            for (NodeZipper<ExecutionResultNode> singleZipper : nodeZippers) {
                HydrationInfo hydrationInfo = new HydrationInfo(field, transformationForField, singleZipper);
                result.add(hydrationInfo);
            }
        }
        return result;
    }

    private CompletableFuture<List<NodeZipper<ExecutionResultNode>>> executeHydration(ExecutionContext context, List<NodeZipper<ExecutionResultNode>> hydrationInputsZippers, List<HydrationTransformation> hydrationTransformations) {
        List<HydrationInfo> infos = getHydrationInfos(hydrationInputsZippers, hydrationTransformations);
        List<CompletableFuture<NodeZipper<ExecutionResultNode>>> resolvedNodeCFs = new ArrayList<>();
        for (HydrationInfo info : infos) {
            resolvedNodeCFs.add(makeHydrationCall(context, info.hydrationInputNodeZipper, info.hydrationTransformation));
        }
        return Async.each(resolvedNodeCFs);
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

    private CompletableFuture<NodeZipper<ExecutionResultNode>> makeHydrationCall(ExecutionContext context,
                                                                                 NodeZipper<ExecutionResultNode> hydrationInputZipper,
                                                                                 HydrationTransformation hydrationTransformation) {
        HydrationInputNode hydrationInputNode = (HydrationInputNode) hydrationInputZipper.getCurNode();
        Field originalField = hydrationTransformation.getOriginalField();
        InnerServiceHydration innerServiceHydration = hydrationTransformation.getInnerServiceHydration();
        String topLevelFieldName = innerServiceHydration.getTopLevelField();

        // TODO: just assume String arguments at the moment
        RemoteArgumentDefinition remoteArgumentDefinition = innerServiceHydration.getArguments().get(0);
        Object value = hydrationInputNode.getFetchedValueAnalysis().getFetchedValue().getFetchedValue();
        Argument argument = Argument.newArgument().name(remoteArgumentDefinition.getName()).value(new StringValue(value.toString())).build();

        OverallQueryTransformer.QueryTransformResult queryTransformResult = queryTransformer.transform(context,
                originalField.getSelectionSet(),
                hydrationTransformation.getFieldType());
        SelectionSet transformedSelectionSet = queryTransformResult.transformedSelectionSet;


        Field topLevelField = newField(topLevelFieldName).selectionSet(transformedSelectionSet)
                .arguments(singletonList(argument))
                .build();

        MergedField transformedMergedField = MergedField.newMergedField(topLevelField).build();


        Service service = getService(innerServiceHydration);
        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters()
                .query(queryTransformResult.document)
                .build();


        ExecutionStepInfo rootExecutionStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema());
        Map<Field, FieldTransformation> transformationByResultField = queryTransformResult.transformationByResultField;
        return serviceExecution.execute(delegatedExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(context, delegatedExecutionResult, rootExecutionStepInfo, singletonList(transformedMergedField), underlyingSchema))
                .thenApply(resultNode -> mergeHydrationResultIntoOverallResult(hydrationTransformation, hydrationInputZipper, resultNode, transformationByResultField))
                .thenCompose(resultNodeZipper -> {

                    List<HydrationTransformation> hydrationTransformations = getHydrationTransformations(queryTransformResult.transformationByResultField.values());
                    List<NodeZipper<ExecutionResultNode>> nextHydrationInputs = getHydrationInputNodes(singleton(resultNodeZipper.getCurNode()));
                    return executeHydration(context, nextHydrationInputs, hydrationTransformations).thenApply(resolvedNodes -> {
                        NodeMultiZipper<ExecutionResultNode> multiZipper = new NodeMultiZipper<>(resultNodeZipper.getCurNode(), resolvedNodes, RESULT_NODE_ADAPTER);
                        ExecutionResultNode newNode = multiZipper.toRootNode();
                        return resultNodeZipper.withNewNode(newNode);
                    });
                });
    }

    private NodeZipper<ExecutionResultNode> mergeHydrationResultIntoOverallResult(HydrationTransformation hydrationTransformation,
                                                                                  NodeZipper<ExecutionResultNode> hydrationInputZipper,
                                                                                  RootExecutionResultNode resultNode,
                                                                                  Map<Field, FieldTransformation> transformationByResultField) {
        RootExecutionResultNode overallResultNode = serviceResultNodesToOverallResult.convert(resultNode, overallSchema, transformationByResultField);
        ExecutionResultNode topLevelResultNode = overallResultNode.getChildren().get(0);
        String oldName = hydrationInputZipper.getBreadcrumbs().get(0).getLocation().getName();
        NodeZipper<ExecutionResultNode> parentNodeZipper = hydrationInputZipper.moveUp();
        ObjectExecutionResultNode parentNode = (ObjectExecutionResultNode) parentNodeZipper.getCurNode();
        Map<String, ExecutionResultNode> namedChildren = parentNode.getChildrenMap();
        List<NamedResultNode> newChildren = new ArrayList<>();
        for (String key : namedChildren.keySet()) {
            if (!key.equals(oldName)) {
                newChildren.add(new NamedResultNode(key, namedChildren.get(key)));
            }
        }
        newChildren.add(new NamedResultNode(FieldUtils.resultKeyForField(hydrationTransformation.getOriginalField()), topLevelResultNode));
        return parentNodeZipper.withNewNode(parentNode.withChildren(newChildren));
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
                                                                   OverallQueryTransformer.QueryTransformResult queryTransformerResult,
                                                                   Service service,
                                                                   ExecutionStepInfo rootExecutionStepInfo) {

        Document query = queryTransformerResult.document;
        Map<Field, FieldTransformation> transformationByResultField = queryTransformerResult.transformationByResultField;
        List<MergedField> transformedMergedFields = queryTransformerResult.transformedMergedFields;

        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters()
                .query(query)
                .build();
        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        return serviceExecution.execute(delegatedExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(context, delegatedExecutionResult, rootExecutionStepInfo, transformedMergedFields, underlyingSchema))
                .thenApply(resultNode -> serviceResultNodesToOverallResult.convert(resultNode, overallSchema, transformationByResultField));

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
