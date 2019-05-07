package graphql.nadel.engine;

import graphql.GraphQLError;
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
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.engine.tracking.FieldTracking;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotEmpty;
import static graphql.nadel.engine.ArtificialFieldUtils.removeArtificialFields;
import static graphql.util.FpKit.map;
import static java.lang.String.format;
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
    private final HydrationInputResolver hydrationInputResolver;

    public NadelExecutionStrategy(List<Service> services, FieldInfos fieldInfos, GraphQLSchema overallSchema, NadelInstrumentation instrumentation) {
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
        assertNotEmpty(services);
        this.services = services;
        this.fieldInfos = fieldInfos;
        this.serviceExecutor = new ServiceExecutor(overallSchema, instrumentation);
        this.hydrationInputResolver = new HydrationInputResolver(services, fieldInfos, overallSchema, instrumentation, serviceExecutor);
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
            Map<String, FieldTransformation> transformationByResultField = queryTransformerResult.getTransformationByResultField();
            Map<String, String> typeRenameMappings = queryTransformerResult.getTypeRenameMappings();

            //
            // say they are dispatched
            fieldTracking.fieldsDispatched(stepInfos);
            //
            // now call put to the service with the new query
            CompletableFuture<RootExecutionResultNode> executeResult = serviceExecutor.execute(executionContext, queryTransformerResult, service, operation);
            CompletableFuture<RootExecutionResultNode> convertedResult = executeResult
                    .thenApply(resultNode -> (RootExecutionResultNode) serviceResultNodesToOverallResult
                            .convert(resultNode, overallSchema, rootExecutionStepInfo, transformationByResultField, typeRenameMappings));

            //
            // and then they are done call back on field tracking that they have completed (modulo hydrated ones).  This is per service call
            convertedResult.whenComplete(fieldTracking::fieldsCompleted);

            resultNodes.add(convertedResult);
        }

        CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);
        return rootResult
                .thenCompose(
                        //
                        // all the nodes that are hydrated need to make new service calls to get their eventual value
                        //
                        rootExecutionResultNode -> hydrationInputResolver.resolveAllHydrationInputs(executionContext, fieldTracking, rootExecutionResultNode)
                                //
                                .thenApply(resultNode -> removeArtificialFields(getNadelContext(executionContext), resultNode))
                                .thenApply(RootExecutionResultNode.class::cast))
                .whenComplete(this::possiblyLogException);
    }


    @SuppressWarnings("unused")
    private <T> void possiblyLogException(T result, Throwable exception) {
        if (exception != null) {
            exception.printStackTrace();
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


