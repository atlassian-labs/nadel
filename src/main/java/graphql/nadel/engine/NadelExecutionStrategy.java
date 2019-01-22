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
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.nadel.DelegatedExecution;
import graphql.nadel.DelegatedExecutionParameters;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Service;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.util.FpKit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotEmpty;
import static graphql.nadel.DelegatedExecutionParameters.newDelegatedExecutionParameters;

@Internal
public class NadelExecutionStrategy implements ExecutionStrategy {

    private final DelegatedResultToResultNode resultToResultNode = new DelegatedResultToResultNode();
    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ResultNodesToOverallResult resultNodesToOverallResult = new ResultNodesToOverallResult();

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
                (service, mergedFields) -> delegate(context, mergedFields, service, fieldSubSelection.getExecutionStepInfo()));

        return mergeTrees(resultNodes);
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

    private CompletableFuture<RootExecutionResultNode> delegate(ExecutionContext context,
                                                                List<MergedField> mergedFields,
                                                                Service service,
                                                                ExecutionStepInfo rootExecutionStepInfo) {

        OverallQueryTransformer queryTransformer = new OverallQueryTransformer(context);
        List<Field> fields = mergedFields.stream()
                .map(MergedField::getSingleField)
                .collect(Collectors.toList());
        queryTransformer.transform(fields, OperationDefinition.Operation.QUERY);
        Document query = queryTransformer.delegateDocument();

        DelegatedExecutionParameters delegatedExecutionParameters = newDelegatedExecutionParameters()
                .query(query)
                .build();
        DelegatedExecution delegatedExecution = service.getDelegatedExecution();
        return delegatedExecution.delegate(delegatedExecutionParameters)
                .thenApply(delegatedExecutionResult -> resultToResultNode.resultToResultNode(context, delegatedExecutionResult, rootExecutionStepInfo, mergedFields))
                .thenApply(resultNode -> resultNodesToOverallResult.convert(resultNode, overallSchema));

    }

}
