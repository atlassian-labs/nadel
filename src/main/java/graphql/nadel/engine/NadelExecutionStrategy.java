package graphql.nadel.engine;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.MergedField;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.engine.tracking.FieldTracking;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.hooks.CreateServiceContextParams;
import graphql.nadel.hooks.ResultRewriteParams;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.util.Data;
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
import static java.util.Collections.singletonList;

@Internal
public class NadelExecutionStrategy {

    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult();
    private final OverallQueryTransformer queryTransformer = new OverallQueryTransformer();

    private final FieldInfos fieldInfos;
    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final ServiceExecutor serviceExecutor;
    private final HydrationInputResolver hydrationInputResolver;
    private final ServiceExecutionHooks serviceExecutionHooks;

    private static final Logger log = LoggerFactory.getLogger(NadelExecutionStrategy.class);

    public NadelExecutionStrategy(List<Service> services,
                                  FieldInfos fieldInfos,
                                  GraphQLSchema overallSchema,
                                  NadelInstrumentation instrumentation,
                                  ServiceExecutionHooks serviceExecutionHooks) {
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
        assertNotEmpty(services);
        this.fieldInfos = fieldInfos;
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.serviceExecutor = new ServiceExecutor(overallSchema, instrumentation);
        this.hydrationInputResolver = new HydrationInputResolver(services, overallSchema, serviceExecutor, serviceExecutionHooks);
    }

    public CompletableFuture<RootExecutionResultNode> execute(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        long startTime = System.currentTimeMillis();
        ExecutionStepInfo rootExecutionStepInfo = fieldSubSelection.getExecutionStepInfo();
        NadelContext nadelContext = getNadelContext(executionContext);

        FieldTracking fieldTracking = new FieldTracking(instrumentation, executionContext);

        Operation operation = Operation.fromAst(executionContext.getOperationDefinition().getOperation());

        CompletableFuture<List<OneServiceExecution>> oneServiceExecutionsCF = prepareServiceExecution(executionContext, fieldSubSelection, rootExecutionStepInfo);
        return oneServiceExecutionsCF.thenCompose(oneServiceExecutions -> {
            List<CompletableFuture<RootExecutionResultNode>> resultNodes =
                    executeTopLevelFields(executionContext, rootExecutionStepInfo, nadelContext, fieldTracking, operation, oneServiceExecutions);

            CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);
            return rootResult
                    .thenApply(resultNode -> removeArtificialFieldsFromRoot(resultNode, nadelContext))
                    .thenCompose(
                            //
                            // all the nodes that are hydrated need to make new service calls to get their eventual value
                            //
                            rootExecutionResultNode -> hydrationInputResolver.resolveAllHydrationInputs(executionContext, fieldTracking, rootExecutionResultNode)
                                    //
                                    .thenApply(resultNode -> removeArtificialFieldsFromRoot(resultNode, nadelContext)))
                    .whenComplete((resultNode, throwable) -> {
                        possiblyLogException(resultNode, throwable);
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        log.debug("NadelExecutionStrategy time: {} ms, executionId: {}", elapsedTime, executionContext.getExecutionId());
                    });
        }).whenComplete(this::possiblyLogException);


    }

    private List<CompletableFuture<RootExecutionResultNode>> executeTopLevelFields(ExecutionContext executionContext, ExecutionStepInfo rootExecutionStepInfo, NadelContext nadelContext, FieldTracking fieldTracking, Operation operation, List<OneServiceExecution> oneServiceExecutions) {
        List<CompletableFuture<RootExecutionResultNode>> resultNodes = new ArrayList<>();
        for (OneServiceExecution oneServiceExecution : oneServiceExecutions) {
            Service service = oneServiceExecution.service;
            ExecutionStepInfo stepInfo = oneServiceExecution.stepInfo;
            Object serviceContext = oneServiceExecution.serviceContext;

            String operationName = buildOperationName(service, executionContext);
            MergedField mergedField = stepInfo.getField();

            //
            // take the original query and transform it into the underlying query needed for that top level field
            //
            GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
            QueryTransformationResult queryTransformInitial = queryTransformer
                    .transformMergedFields(executionContext, underlyingSchema, operationName, operation, singletonList(mergedField), serviceExecutionHooks, service, serviceContext);

            CompletableFuture<Data> dataCF = CompletableFuture.completedFuture(Data.of(queryTransformInitial, executionContext, stepInfo));


            CompletableFuture<RootExecutionResultNode> serviceResult = dataCF.thenCompose(data -> {
                ExecutionContext runExecutionCtx = data.get(ExecutionContext.class);
                ExecutionStepInfo esi = data.get(ExecutionStepInfo.class);
                QueryTransformationResult queryTransform = data.get(QueryTransformationResult.class);

                Map<String, FieldTransformation> transformationByResultField = queryTransform.getTransformationByResultField();
                Map<String, String> typeRenameMappings = queryTransform.getTypeRenameMappings();

                ExecutionContext newExecutionContext = buildServiceVariableOverrides(runExecutionCtx, queryTransform.getVariableValues());

                //
                // say the fields are dispatched
                fieldTracking.fieldsDispatched(singletonList(esi));
                //
                // now call put to the service with the new query
                CompletableFuture<RootExecutionResultNode> serviceCallResult = serviceExecutor
                        .execute(newExecutionContext, queryTransform, service, operation, serviceContext);

                CompletableFuture<RootExecutionResultNode> convertedResult = serviceCallResult
                        .thenApply(resultNode -> (RootExecutionResultNode) serviceResultNodesToOverallResult
                                .convert(newExecutionContext.getExecutionId(),
                                        nadelContext.getForkJoinPool(),
                                        resultNode,
                                        overallSchema,
                                        rootExecutionStepInfo,
                                        transformationByResultField,
                                        typeRenameMappings));

                //
                // and then they are done call back on field tracking that they have completed (modulo hydrated ones).  This is per service call
                convertedResult = convertedResult
                        .whenComplete(fieldTracking::fieldsCompleted);

                return convertedResult
                        .thenCompose(rootResultNode -> {
                            ResultRewriteParams resultRewriteParams = ResultRewriteParams.newParameters()
                                    .from(runExecutionCtx)
                                    .service(service)
                                    .serviceContext(serviceContext)
                                    .executionStepInfo(esi)
                                    .resultNode(rootResultNode)
                                    .build();
                            return serviceExecutionHooks.resultRewrite(resultRewriteParams);
                        });
            });


            resultNodes.add(serviceResult);
        }
        return resultNodes;
    }

    private MergedField getTopLevelFieldFromDoc(Document document) {
        OperationDefinition operationDefinition = document.getDefinitionsOfType(OperationDefinition.class).get(0);
        Assert.assertNotNull(operationDefinition);

        Field field = operationDefinition.getSelectionSet().getSelectionsOfType(Field.class).get(0);
        Assert.assertNotNull(field);
        return MergedField.newMergedField(field).build();
    }

    private RootExecutionResultNode removeArtificialFieldsFromRoot(ExecutionResultNode resultNode, NadelContext nadelContext) {
        return (RootExecutionResultNode) removeArtificialFields(nadelContext, resultNode);
    }


    @SuppressWarnings("unused")
    private <T> void possiblyLogException(T result, Throwable exception) {
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    private ExecutionContext buildServiceVariableOverrides(ExecutionContext executionContext, Map<String, Object> overrideVariables) {
        if (!overrideVariables.isEmpty()) {
            Map<String, Object> newVariables = mergeVariables(executionContext.getVariables(), overrideVariables);
            executionContext = executionContext.transform(builder -> builder.variables(newVariables));
        }
        return executionContext;
    }

    private Map<String, Object> mergeVariables(Map<String, Object> variables, Map<String, Object> overrideVariables) {
        Map<String, Object> newVariables = new LinkedHashMap<>(variables);
        newVariables.putAll(overrideVariables);
        return newVariables;
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

    private static class OneServiceExecution {

        public OneServiceExecution(Service service, Object serviceContext, ExecutionStepInfo stepInfo) {
            this.service = service;
            this.serviceContext = serviceContext;
            this.stepInfo = stepInfo;
        }

        final Service service;
        final Object serviceContext;
        final ExecutionStepInfo stepInfo;
    }

    private CompletableFuture<List<OneServiceExecution>> prepareServiceExecution(ExecutionContext executionCtx, FieldSubSelection fieldSubSelection, ExecutionStepInfo rootExecutionStepInfo) {
        List<CompletableFuture<OneServiceExecution>> result = new ArrayList<>();
        for (MergedField mergedField : fieldSubSelection.getMergedSelectionSet().getSubFieldsList()) {
            ExecutionStepInfo fieldExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(executionCtx, mergedField, rootExecutionStepInfo);
            Service service = getServiceForFieldDefinition(fieldExecutionStepInfo.getFieldDefinition());

            CreateServiceContextParams parameters = CreateServiceContextParams.newParameters()
                    .from(executionCtx)
                    .service(service)
                    .executionStepInfo(fieldExecutionStepInfo)
                    .build();

            CompletableFuture<Object> serviceContextCF = serviceExecutionHooks.createServiceContext(parameters);
            CompletableFuture<OneServiceExecution> serviceCF = serviceContextCF.thenApply(serviceContext -> new OneServiceExecution(service, serviceContext, fieldExecutionStepInfo));
            result.add(serviceCF);
        }
        return Async.each(result);
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


