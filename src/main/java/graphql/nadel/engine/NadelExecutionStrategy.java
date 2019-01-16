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
import graphql.language.Document;
import graphql.nadel.DelegatedExecution;
import graphql.nadel.DelegatedExecutionParameters;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Service;
import graphql.schema.GraphQLFieldDefinition;
import graphql.util.FpKit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotEmpty;
import static graphql.nadel.DelegatedExecutionParameters.newDelegatedExecutionParameters;

@Internal
public class NadelExecutionStrategy implements ExecutionStrategy {

    private DelegatedResultToResultNode resultToResultNode = new DelegatedResultToResultNode();
    private ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();

    private FieldInfos fieldInfos;

    public NadelExecutionStrategy(List<Service> services, FieldInfos fieldInfos) {
        assertNotEmpty(services);
        this.fieldInfos = fieldInfos;
    }

    @Override
    public CompletableFuture<RootExecutionResultNode> execute(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        Map<DelegatedExecution, List<MergedField>> delegatedExecutionForTopLevel = getDelegatedExecutionForTopLevel(context, fieldSubSelection);

        List<CompletableFuture<RootExecutionResultNode>> resultNodes = FpKit.mapEntries(delegatedExecutionForTopLevel,
                (delegatedExecution, mergedFields) -> delegate(context, mergedFields, delegatedExecution, fieldSubSelection.getExecutionStepInfo()));

        return mergeTrees(resultNodes);
    }

    private CompletableFuture<RootExecutionResultNode> mergeTrees(List<CompletableFuture<RootExecutionResultNode>> resultNodes) {
        return Async.each(resultNodes).thenApply(rootNodes -> {
            Map<String, ExecutionResultNode> mergedChildren = new LinkedHashMap<>();
            rootNodes.forEach(rootNode -> mergedChildren.putAll(rootNode.getChildrenMap()));
            return new RootExecutionResultNode(mergedChildren);
        });
    }

    private Map<DelegatedExecution, List<MergedField>> getDelegatedExecutionForTopLevel(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        //TODO: consider dynamic delegation targets in the future
        Map<DelegatedExecution, List<MergedField>> result = new LinkedHashMap<>();
        ExecutionStepInfo executionStepInfo = fieldSubSelection.getExecutionStepInfo();
        for (MergedField mergedField : fieldSubSelection.getMergedSelectionSet().getSubFieldsList()) {
            ExecutionStepInfo newExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(context, mergedField, executionStepInfo);
            DelegatedExecution delegate = getDelegatedExecutionForFieldDefinition(newExecutionStepInfo.getFieldDefinition());
            result.computeIfAbsent(delegate, key -> new ArrayList<>());
            result.get(delegate).add(mergedField);
        }
        return result;
    }

    private DelegatedExecution getDelegatedExecutionForFieldDefinition(GraphQLFieldDefinition fieldDefinition) {
        FieldInfo info = fieldInfos.getInfo(fieldDefinition);
        return info.getService().getDelegatedExecution();
    }

    private CompletableFuture<RootExecutionResultNode> delegate(ExecutionContext context,
                                                                List<MergedField> mergedFields,
                                                                DelegatedExecution delegatedExecution,
                                                                ExecutionStepInfo executionStepInfo) {
        Document query = SourceQueryTransformer.transform(mergedFields, context, executionStepInfo);

        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters()
                .query(query)
                .build();
        return delegatedExecution.delegate(delegatedExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(context, delegatedExecutionResult, executionStepInfo, mergedFields));
    }

}
