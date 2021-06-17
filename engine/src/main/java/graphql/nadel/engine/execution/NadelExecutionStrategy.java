package graphql.nadel.engine.execution;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.nadel.OperationKind;
import graphql.nadel.Service;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.engine.BenchmarkContext;
import graphql.nadel.engine.FieldInfo;
import graphql.nadel.engine.FieldInfos;
import graphql.nadel.engine.NadelContext;
import graphql.nadel.engine.execution.transformation.FieldTransformation;
import graphql.nadel.engine.execution.transformation.TransformationMetadata.NormalizedFieldAndError;
import graphql.nadel.engine.hooks.EngineServiceExecutionHooks;
import graphql.nadel.engine.hooks.ResultRewriteParams;
import graphql.nadel.engine.result.LeafExecutionResultNode;
import graphql.nadel.engine.result.ResultComplexityAggregator;
import graphql.nadel.engine.result.RootExecutionResultNode;
import graphql.nadel.hooks.CreateServiceContextParams;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.hooks.ServiceOrError;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.nadel.schema.NadelDirectives;
import graphql.nadel.util.MergedFieldUtil;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotEmpty;
import static graphql.Assert.assertNotNull;
import static graphql.nadel.schema.NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

@Internal
public class NadelExecutionStrategy {

    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceResultNodesToOverallResult serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResult();
    private final OverallQueryTransformer queryTransformer = new OverallQueryTransformer();
    private final ServiceResultToResultNodes resultToResultNode = new ServiceResultToResultNodes();

    private final FieldInfos fieldInfos;
    private final GraphQLSchema overallSchema;
    private final ServiceExecutor serviceExecutor;
    private final HydrationInputResolver hydrationInputResolver;
    private final ServiceExecutionHooks serviceExecutionHooks;
    private final ExecutionPathSet hydrationInputPaths;
    private final List<Service> services;

    private static final Logger log = LoggerFactory.getLogger(NadelExecutionStrategy.class);

    public NadelExecutionStrategy(List<Service> services,
                                  FieldInfos fieldInfos,
                                  GraphQLSchema overallSchema,
                                  NadelInstrumentation instrumentation,
                                  ServiceExecutionHooks serviceExecutionHooks) {
        this.overallSchema = overallSchema;
        assertNotEmpty(services);
        this.fieldInfos = fieldInfos;
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.serviceExecutor = new ServiceExecutor(instrumentation);
        this.hydrationInputPaths = new ExecutionPathSet();
        this.hydrationInputResolver = new HydrationInputResolver(services, overallSchema, serviceExecutor, serviceExecutionHooks, hydrationInputPaths);
        this.services = services;
    }

    public CompletableFuture<RootExecutionResultNode> execute(ExecutionContext executionContext, FieldSubSelection fieldSubSelection, ResultComplexityAggregator resultComplexityAggregator) {
        long startTime = System.currentTimeMillis();
        ExecutionStepInfo rootExecutionStepInfo = fieldSubSelection.getExecutionStepInfo();
        NadelContext nadelContext = getNadelContext(executionContext);
        OperationKind operationKind = OperationKind.fromAst(executionContext.getOperationDefinition().getOperation());
        CompletableFuture<List<OneServiceExecution>> oneServiceExecutionsCF = prepareServiceExecution(executionContext, fieldSubSelection, rootExecutionStepInfo);

        return oneServiceExecutionsCF.thenCompose(oneServiceExecutions -> {
            Map<Service, Object> serviceContextsByService = serviceContextsByService(oneServiceExecutions);
            List<CompletableFuture<RootExecutionResultNode>> resultNodes =
                    executeTopLevelFields(executionContext, nadelContext, operationKind, oneServiceExecutions, resultComplexityAggregator, hydrationInputPaths);

            CompletableFuture<RootExecutionResultNode> rootResult = mergeTrees(resultNodes);
            return rootResult
                    .thenCompose(
                            //
                            // all the nodes that are hydrated need to make new service calls to get their eventual value
                            //
                            rootExecutionResultNode -> hydrationInputResolver.resolveAllHydrationInputs(executionContext, rootExecutionResultNode, serviceContextsByService, resultComplexityAggregator)
                                    .thenApply(resultNode -> (RootExecutionResultNode) resultNode))
                    .whenComplete((resultNode, throwable) -> {
                        possiblyLogException(resultNode, throwable);
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        log.debug("NadelExecutionStrategy time: {} ms, executionId: {}", elapsedTime, executionContext.getExecutionId());
                    });
        }).whenComplete(this::possiblyLogException);
    }

    private Map<Service, Object> serviceContextsByService(List<OneServiceExecution> oneServiceExecutions) {
        Map<Service, Object> result = new LinkedHashMap<>();
        for (OneServiceExecution oneServiceExecution : oneServiceExecutions) {
            result.put(oneServiceExecution.service, oneServiceExecution.serviceContext);
        }
        return result;
    }

    private CompletableFuture<List<OneServiceExecution>> prepareServiceExecution(ExecutionContext executionCtx, FieldSubSelection fieldSubSelection, ExecutionStepInfo rootExecutionStepInfo) {
        List<CompletableFuture<OneServiceExecution>> result = new ArrayList<>();
        for (MergedField mergedField : fieldSubSelection.getMergedSelectionSet().getSubFieldsList()) {
            ExecutionStepInfo fieldExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(executionCtx, mergedField, rootExecutionStepInfo);
            boolean isNamespaced = !fieldExecutionStepInfo.getFieldDefinition().getDirectives(NAMESPACED_DIRECTIVE_DEFINITION.getName()).isEmpty();
            if (isNamespaced) {
                result.addAll(getServiceExecutionsForNamespacedField(executionCtx, rootExecutionStepInfo, mergedField, fieldExecutionStepInfo));
            }
            else {
                boolean usesDynamicService = fieldExecutionStepInfo.getFieldDefinition().getDirectives()
                        .stream()
                        .anyMatch(directive -> directive.getName().equals(NadelDirectives.DYNAMIC_SERVICE_DIRECTIVE_DEFINITION.getName()));
                final Service service;
                if (usesDynamicService) {
                    ServiceOrError serviceOrError = assertNotNull(serviceExecutionHooks.resolveServiceForField(services, fieldExecutionStepInfo));

                    if (serviceOrError .getService() != null) {
                        service = serviceOrError.getService();
                    } else {
                        GraphQLError graphQLError = assertNotNull(serviceOrError.getError());
                        result.add(CompletableFuture.completedFuture(new OneServiceExecution(null, null, null, true, graphQLError, fieldExecutionStepInfo)));
                    continue;
                    }

                } else {
                    service = getServiceForFieldDefinition(fieldExecutionStepInfo.getFieldDefinition());
                }

                result.add(getOneServiceExecution(executionCtx, fieldExecutionStepInfo, service));
            }
        }
        return Async.each(result);
    }

    private List<CompletableFuture<OneServiceExecution>> getServiceExecutionsForNamespacedField(
            ExecutionContext executionCtx, ExecutionStepInfo rootExecutionStepInfo, MergedField mergedField, ExecutionStepInfo stepInfoForNamespacedField
    ) {
        ArrayList<CompletableFuture<OneServiceExecution>> serviceExecutions = new ArrayList<>();
        Assert.assertTrue(
                stepInfoForNamespacedField.getUnwrappedNonNullType() instanceof GraphQLObjectType,
                () -> "field annotated with @namespaced directive is expected to be of GraphQLObjectType");

        GraphQLObjectType namespacedObjectType = (GraphQLObjectType) stepInfoForNamespacedField.getUnwrappedNonNullType();
        for (Map.Entry<Service, Set<GraphQLFieldDefinition>> entry : fieldInfos.fieldDefinitionsByService.entrySet()) {
            Service service = entry.getKey();
            Set<GraphQLFieldDefinition> secondLevelFieldDefinitionsForService = entry.getValue();

            Optional<MergedField> maybeNewMergedField = MergedFieldUtil.includeSubSelection(mergedField, namespacedObjectType, executionCtx,
                    field -> secondLevelFieldDefinitionsForService.stream()
                            .filter(namespacedObjectType.getFieldDefinitions()::contains)
                            .anyMatch(graphQLFieldDefinition -> graphQLFieldDefinition.getName().equals(field.getName())));
            maybeNewMergedField.ifPresent(newMergedField -> {
                ExecutionStepInfo newFieldExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(executionCtx, newMergedField, rootExecutionStepInfo);
                serviceExecutions.add(getOneServiceExecution(executionCtx, newFieldExecutionStepInfo, service));
            });
        }
        return serviceExecutions;
    }

    private CompletableFuture<OneServiceExecution> getOneServiceExecution(ExecutionContext executionCtx, ExecutionStepInfo fieldExecutionStepInfo, Service service) {
        CreateServiceContextParams parameters = CreateServiceContextParams.newParameters()
                .from(executionCtx)
                .service(service)
                .executionStepInfo(fieldExecutionStepInfo)
                .build();

        CompletableFuture<Object> serviceContextCF = serviceExecutionHooks.createServiceContext(parameters);
        return serviceContextCF.thenApply(serviceContext -> new OneServiceExecution(service, serviceContext, fieldExecutionStepInfo, false, null, fieldExecutionStepInfo));
    }

    private List<CompletableFuture<RootExecutionResultNode>> executeTopLevelFields(
            ExecutionContext executionContext,
            NadelContext nadelContext,
            OperationKind operationKind,
            List<OneServiceExecution> oneServiceExecutions,
            ResultComplexityAggregator resultComplexityAggregator,
            Set<ResultPath> hydrationInputPaths) {

        List<CompletableFuture<RootExecutionResultNode>> resultNodes = new ArrayList<>();
        for (OneServiceExecution oneServiceExecution : oneServiceExecutions) {
            if(oneServiceExecution.earlyFailure) {
                LeafExecutionResultNode leafExecutionResultNode = new LeafExecutionResultNode
                        .Builder()
                        .addError(oneServiceExecution.error)
                        .completedValue(null)
                        .fieldDefinition(oneServiceExecution.fieldExecutionStepInfo.getFieldDefinition())
                        .resultPath(oneServiceExecution.fieldExecutionStepInfo.getPath())
                        .alias(oneServiceExecution.fieldExecutionStepInfo.getField().getSingleField().getAlias())
                        .objectType(oneServiceExecution.fieldExecutionStepInfo.getObjectType())
                        .build();

                RootExecutionResultNode rootExecutionResultNode = new RootExecutionResultNode.Builder()
                        .addChild(leafExecutionResultNode)
                        .build();

                resultNodes.add(CompletableFuture.completedFuture(rootExecutionResultNode));

                continue;
            }

            Service service = oneServiceExecution.service;
            ExecutionStepInfo esi = oneServiceExecution.stepInfo;
            Object serviceContext = oneServiceExecution.serviceContext;

            String operationName = buildOperationName(service, executionContext);
            MergedField mergedField = esi.getField();

            //
            // take the original query and transform it into the underlying query needed for that top level field
            //
            GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
            CompletableFuture<QueryTransformationResult> transformedQueryCF = queryTransformer
                    .transformMergedFields(executionContext, underlyingSchema, operationName, operationKind, singletonList(mergedField), serviceExecutionHooks, service, serviceContext);

            resultNodes.add(transformedQueryCF.thenCompose(transformedQuery -> {
                Map<String, FieldTransformation> transformationIdToTransformation = transformedQuery.getTransformations().getTransformationIdToTransformation();
                Map<String, String> typeRenameMappings = transformedQuery.getTransformations().getTypeRenameMappings();
                Map<FieldTransformation, String> transformationToFieldId = transformedQuery.getTransformations().getTransformationToFieldId();

                ExecutionContext newExecutionContext = buildServiceVariableOverrides(executionContext, transformedQuery.getVariableValues());

                Optional<GraphQLError> maybeFieldForbiddenError = getForbiddenTopLevelFieldError(esi, transformedQuery);
                // If field is forbidden, do NOT execute it
                if (maybeFieldForbiddenError.isPresent()) {
                    GraphQLError fieldForbiddenError = maybeFieldForbiddenError.get();
                    return CompletableFuture.completedFuture(getForbiddenTopLevelFieldResult(nadelContext, esi, fieldForbiddenError));
                }

                CompletableFuture<RootExecutionResultNode> convertedResult;

                if (skipTransformationProcessing(transformedQuery)) {
                    convertedResult = serviceExecutor
                            .execute(newExecutionContext, transformedQuery, service, operationKind, serviceContext, overallSchema, false);
                    resultComplexityAggregator.incrementServiceNodeCount(service.getName(), 0);
                } else {
                    CompletableFuture<RootExecutionResultNode> serviceCallResult = serviceExecutor
                            .execute(newExecutionContext, transformedQuery, service, operationKind, serviceContext, service.getUnderlyingSchema(), false);
                    convertedResult = serviceCallResult
                            .thenApply(resultNode -> {
                                if (nadelContext.getUserSuppliedContext() instanceof BenchmarkContext) {
                                    BenchmarkContext benchmarkContext = (BenchmarkContext) nadelContext.getUserSuppliedContext();
                                    benchmarkContext.serviceResultNodesToOverallResult.executionId = newExecutionContext.getExecutionId();
                                    benchmarkContext.serviceResultNodesToOverallResult.resultNode = resultNode;
                                    benchmarkContext.serviceResultNodesToOverallResult.overallSchema = overallSchema;
                                    benchmarkContext.serviceResultNodesToOverallResult.correctRootNode = resultNode;
                                    benchmarkContext.serviceResultNodesToOverallResult.transformationIdToTransformation = transformationIdToTransformation;
                                    benchmarkContext.serviceResultNodesToOverallResult.typeRenameMappings = typeRenameMappings;
                                    benchmarkContext.serviceResultNodesToOverallResult.nadelContext = nadelContext;
                                    benchmarkContext.serviceResultNodesToOverallResult.transformationMetadata = transformedQuery.getRemovedFieldMap();
                                }
                                return (RootExecutionResultNode) serviceResultNodesToOverallResult
                                        .convert(newExecutionContext.getExecutionId(),
                                                resultNode,
                                                overallSchema,
                                                resultNode,
                                                transformationIdToTransformation,
                                                transformationToFieldId,
                                                typeRenameMappings,
                                                nadelContext,
                                                transformedQuery.getRemovedFieldMap(),
                                                hydrationInputPaths);
                            });

                    // Set the result node count for this service.
                    convertedResult.thenAccept(rootExecutionResultNode -> {
                        resultComplexityAggregator.incrementServiceNodeCount(service.getName(), rootExecutionResultNode.getTotalNodeCount());
                        resultComplexityAggregator.incrementFieldRenameCount(rootExecutionResultNode.getTotalFieldRenameCount());
                        resultComplexityAggregator.incrementTypeRenameCount(rootExecutionResultNode.getTotalTypeRenameCount());
                    });
                }

                CompletableFuture<RootExecutionResultNode> serviceResult = convertedResult;

                if (serviceExecutionHooks instanceof EngineServiceExecutionHooks) {
                    serviceResult = serviceResult.thenCompose(rootResultNode -> {
                        ResultRewriteParams resultRewriteParams = ResultRewriteParams.newParameters()
                                .from(executionContext)
                                .service(service)
                                .serviceContext(serviceContext)
                                .executionStepInfo(esi)
                                .resultNode(rootResultNode)
                                .build();
                        return ((EngineServiceExecutionHooks) serviceExecutionHooks).resultRewrite(resultRewriteParams);
                    });
                }

                return serviceResult;
            }));
        }
        return resultNodes;
    }

    /**
     * A top level field error is present if the field should not be executed and an
     * error should be put in lieu. We check this before calling out to the underlying
     * service. This error is usually present when the field has been forbidden by
     * {@link ServiceExecutionHooks#isFieldForbidden}.
     *
     * @param esi              the {@link ExecutionStepInfo} for the top level field
     * @param transformedQuery the query for that specific top level field
     * @return a {@link GraphQLError} if the field was forbidden before, otherwise empty
     */
    private Optional<GraphQLError> getForbiddenTopLevelFieldError(ExecutionStepInfo esi, QueryTransformationResult transformedQuery) {
        GraphQLFieldDefinition fieldDefinition = esi.getFieldDefinition();
        String topLevelFieldId = NodeId.getId(fieldDefinition);
        return transformedQuery.getRemovedFieldMap()
                .getRemovedFieldById(topLevelFieldId)
                .map(NormalizedFieldAndError::getError);
    }

    /**
     * Creates the {@link RootExecutionResultNode} for a forbidden field. In that
     * case the underlying service should not be called and we would fill the
     * overall GraphQL response with an error for that specific top level field.
     *
     * @param nadelContext context for the execution
     * @param esi          the {@link ExecutionStepInfo} for the top level field
     * @param error        the {@link GraphQLError} to put in the overall response
     * @return {@link RootExecutionResultNode} with the specified top level field nulled out and with the given GraphQL error
     */
    private RootExecutionResultNode getForbiddenTopLevelFieldResult(NadelContext nadelContext, ExecutionStepInfo esi, GraphQLError error) {
        String topLevelFieldResultKey = esi.getResultKey();
        NormalizedQueryFromAst overallQuery = nadelContext.getNormalizedOverallQuery();
        NormalizedQueryField topLevelField = overallQuery.getTopLevelField(topLevelFieldResultKey);
        return resultToResultNode.createResultWithNullTopLevelField(overallQuery, topLevelField, singletonList(error), emptyMap());
    }

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
        return Async.each(resultNodes).thenApply(StrategyUtil::mergeTrees);
    }

    private static class OneServiceExecution {

        public OneServiceExecution(Service service, Object serviceContext, ExecutionStepInfo stepInfo, boolean earlyFailure, GraphQLError error, ExecutionStepInfo fieldExecutionStepInfo) {
            this.service = service;
            this.serviceContext = serviceContext;
            this.stepInfo = stepInfo;
            this.earlyFailure = earlyFailure;
            this.error = error;
            this.fieldExecutionStepInfo = fieldExecutionStepInfo;
        }

        final Service service;
        final Object serviceContext;
        final ExecutionStepInfo stepInfo;
        final boolean earlyFailure;
        final GraphQLError error;
        final ExecutionStepInfo fieldExecutionStepInfo;
    }

    public static class ExecutionPathSet extends LinkedHashSet<ResultPath> {
        @Override
        public boolean add(ResultPath executionPath) {
            ResultPath path = executionPath.getParent();
            while (path != null) {
                super.add(path);
                path = path.getParent();
            }
            return super.add(executionPath);
        }
    }

    private Service getServiceForFieldDefinition(GraphQLFieldDefinition fieldDefinition) {
        FieldInfo info = assertNotNull(fieldInfos.getInfo(fieldDefinition), () -> String.format("no field info for field %s", fieldDefinition.getName()));
        return info.getService();
    }

    private String buildOperationName(Service service, ExecutionContext executionContext) {
        // to help with downstream debugging we put our name and their name in the operation
        NadelContext nadelContext = executionContext.getContext();
        if (nadelContext.getOriginalOperationName() != null) {
            return format("nadel_2_%s_%s", service.getName(), nadelContext.getOriginalOperationName());
        } else {
            return format("nadel_2_%s", service.getName());
        }
    }

    private NadelContext getNadelContext(ExecutionContext executionContext) {
        return executionContext.getContext();
    }

    private boolean skipTransformationProcessing(QueryTransformationResult transformedQuery) {
        TransformationState transformations = transformedQuery.getTransformations();
        return transformations.getTransformationIdToTransformation().isEmpty() &&
                transformations.getTypeRenameMappings().isEmpty() &&
                !transformedQuery.getRemovedFieldMap().hasRemovedFields() &&
                transformations.getHintTypenames().isEmpty();
    }
}
