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
import graphql.execution.nextgen.result.ResultNodeTraverser;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.nadel.DelegatedExecutionParameters;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecution;
import graphql.nadel.engine.transformation.FieldTransformation;
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

import static graphql.Assert.assertNotEmpty;
import static graphql.execution.nextgen.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;
import static graphql.nadel.DelegatedExecutionParameters.newDelegatedExecutionParameters;
import static java.util.Collections.singleton;

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

        List<CompletableFuture<RootExecutionResultNode>> resultNodes = FpKit.mapEntries(delegatedExecutionForTopLevel,
                (service, mergedFields) -> callService(context, mergedFields, service, fieldSubSelection.getExecutionStepInfo()));

        CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);

        return rootResult.thenApply(rootExecutionResultNode -> {
            List<NodeZipper<ExecutionResultNode>> hydrationInputNodes = getHydrationInputNodes(singleton(rootExecutionResultNode));
            return rootExecutionResultNode;
        });
    }

    private CompletableFuture<RootExecutionResultNode> makeHydrationCalls(List<NodeZipper<ExecutionResultNode>> hydrationInputs) {
        // the hydration input
        return null;
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
                                                                   List<MergedField> mergedFields,
                                                                   Service service,
                                                                   ExecutionStepInfo rootExecutionStepInfo) {

        OverallQueryTransformer queryTransformer = new OverallQueryTransformer(context);
        queryTransformer.transform(mergedFields, OperationDefinition.Operation.QUERY);

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
