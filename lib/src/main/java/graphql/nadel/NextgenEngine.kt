package graphql.nadel

import graphql.ErrorType
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.execution.ExecutionIdProvider
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelDefaultIntrospectionRunner
import graphql.nadel.engine.blueprint.NadelExecutionBlueprintFactory
import graphql.nadel.engine.blueprint.NadelIntrospectionRunnerFactory
import graphql.nadel.engine.document.DocumentPredicates
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.plan.NadelExecutionPlanFactory
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.query.DynamicServiceResolution
import graphql.nadel.engine.transform.query.NadelFieldToService
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultTransformer
import graphql.nadel.engine.util.beginExecute
import graphql.nadel.engine.util.compileToDocument
import graphql.nadel.engine.util.copy
import graphql.nadel.engine.util.getOperationKind
import graphql.nadel.engine.util.newExecutionResult
import graphql.nadel.engine.util.newGraphQLError
import graphql.nadel.engine.util.newServiceExecutionErrorResult
import graphql.nadel.engine.util.newServiceExecutionResult
import graphql.nadel.engine.util.provide
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.ErrorData
import graphql.nadel.instrumentation.parameters.ErrorType.ServiceExecutionError
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnErrorParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep.Companion.DocumentCompilation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep
import graphql.nadel.instrumentation.parameters.child
import graphql.nadel.util.OperationNameUtil
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables
import graphql.normalized.VariablePredicate
import graphql.schema.GraphQLSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import graphql.normalized.ExecutableNormalizedOperationFactory.Options.defaultOptions as executableNormalizedOperationFactoryOptions

internal class NextgenEngine(
    private val engineSchema: GraphQLSchema,
    private val querySchema: GraphQLSchema,
    private val instrumentation: NadelInstrumentation,
    private val serviceExecutionHooks: ServiceExecutionHooks,
    private val executionIdProvider: ExecutionIdProvider,
    maxQueryDepth: Int,
    services: List<Service>,
    transforms: List<NadelTransform<out Any>> = emptyList(),
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory = NadelIntrospectionRunnerFactory(::NadelDefaultIntrospectionRunner),
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services: Map<String, Service> = services.strictAssociateBy { it.name }
    private val overallExecutionBlueprint = NadelExecutionBlueprintFactory.create(
        engineSchema = engineSchema,
        services = services,
    )
    private val executionPlanner = NadelExecutionPlanFactory.create(
        executionBlueprint = overallExecutionBlueprint,
        engine = this,
        transforms = transforms,
    )
    private val resultTransformer = NadelResultTransformer(overallExecutionBlueprint)
    private val dynamicServiceResolution = DynamicServiceResolution(
        engineSchema = engineSchema,
        serviceExecutionHooks = serviceExecutionHooks,
        services = services,
    )
    private val fieldToService = NadelFieldToService(
        querySchema = querySchema,
        overallExecutionBlueprint = overallExecutionBlueprint,
        introspectionRunnerFactory = introspectionRunnerFactory,
        dynamicServiceResolution = dynamicServiceResolution,
        services = this.services,
    )

    private val operationParseOptions = executableNormalizedOperationFactoryOptions()
        .maxChildrenDepth(maxQueryDepth)

    fun execute(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?,
        nadelExecutionParams: NadelExecutionParams,
    ): CompletableFuture<ExecutionResult> {
        return coroutineScope.async {
            executeCoroutine(
                executionInput,
                queryDocument,
                instrumentationState,
                nadelExecutionParams.nadelExecutionHints,
            )
        }.asCompletableFuture()
    }

    fun close() {
        // Closes the scope after letting in flight requests go through
        coroutineScope.launch {
            delay(60_000) // Wait a minute
            coroutineScope.cancel()
        }
    }

    private suspend fun executeCoroutine(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?,
        executionHints: NadelExecutionHints,
    ): ExecutionResult {
        try {
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = executionInput.context,
                instrumentationState,
            )

            val query = timer.time(step = RootStep.ExecutableOperationParsing) {
                createExecutableNormalizedOperationWithRawVariables(
                    querySchema,
                    queryDocument,
                    executionInput.operationName,
                    executionInput.rawVariables,
                    operationParseOptions
                        .graphQLContext(executionInput.graphQLContext),
                )
            }

            val executionContext = NadelExecutionContext(
                executionInput,
                query,
                serviceExecutionHooks,
                executionHints,
                instrumentationState,
                timer,
            )
            val beginExecuteContext = instrumentation.beginExecute(
                query,
                queryDocument,
                executionInput,
                engineSchema,
                instrumentationState,
            )

            val result: ExecutionResult = try {
                val fields = fieldToService.getServicesForTopLevelFields(query, executionHints)
                val results = coroutineScope {
                    fields
                        .map { (field, service) ->
                            async {
                                try {
                                    val resolvedService = fieldToService.resolveDynamicService(field, service)
                                    executeTopLevelField(
                                        topLevelField = field,
                                        service = resolvedService,
                                        executionContext = executionContext,
                                    )
                                } catch (e: Throwable) {
                                    when (e) {
                                        is GraphQLError -> newServiceExecutionErrorResult(field, error = e)
                                        else -> throw e
                                    }
                                }
                            }
                        }
                }.awaitAll()

                if (executionHints.newResultMergerAndNamespacedTypename()) {
                    NadelResultMerger.mergeResults(fields, engineSchema, results)
                } else {
                    graphql.nadel.engine.util.mergeResults(results)
                }
            } catch (e: Throwable) {
                beginExecuteContext?.onCompleted(null, e)
                throw e
            }

            beginExecuteContext?.onCompleted(result, null)
            return result
        } catch (e: Throwable) {
            when (e) {
                is GraphQLError -> return newExecutionResult(error = e)
                else -> throw e
            }
        }
    }

    internal suspend fun executeTopLevelField(
        topLevelField: ExecutableNormalizedField,
        service: Service,
        executionContext: NadelExecutionContext,
        serviceHydrationDetails: ServiceExecutionHydrationDetails? = null,
    ): ServiceExecutionResult {
        val timer = executionContext.timer
        val executionPlan = timer.time(step = RootStep.ExecutionPlanning) {
            executionPlanner.create(
                executionContext = executionContext,
                services = services,
                service = service,
                rootField = topLevelField,
                serviceHydrationDetails = serviceHydrationDetails,
            )
        }
        val queryTransform = timer.time(step = RootStep.QueryTransforming) {
            transformQuery(service, executionContext, executionPlan, topLevelField)
        }
        val transformedQuery = queryTransform.result.single()
        val result: ServiceExecutionResult = timer.time(step = RootStep.ServiceExecution.child(service.name)) {
            executeService(
                service = service,
                transformedQuery = transformedQuery,
                executionContext = executionContext,
                executionHydrationDetails = serviceHydrationDetails,
            )
        }
        val transformedResult: ServiceExecutionResult = when {
            topLevelField.name.startsWith("__") -> result
            else -> timer.time(step = RootStep.ResultTransforming) {
                resultTransformer.transform(
                    executionContext = executionContext,
                    executionPlan = executionPlan,
                    artificialFields = queryTransform.artificialFields,
                    overallToUnderlyingFields = queryTransform.overallToUnderlyingFields,
                    service = service,
                    result = result,
                )
            }
        }

        return transformedResult
    }

    private suspend fun executeService(
        service: Service,
        transformedQuery: ExecutableNormalizedField,
        executionContext: NadelExecutionContext,
        executionHydrationDetails: ServiceExecutionHydrationDetails? = null,
    ): ServiceExecutionResult {
        val timer = executionContext.timer

        val executionInput = executionContext.executionInput

        val jsonPredicate: VariablePredicate = getDocumentVariablePredicate(executionContext.hints, service)

        val compileResult = timer.time(step = DocumentCompilation) {
            compileToDocument(
                schema = service.underlyingSchema,
                operationKind = transformedQuery.getOperationKind(engineSchema),
                operationName = getOperationName(service, executionContext),
                topLevelFields = listOf(transformedQuery),
                variablePredicate = jsonPredicate
            )
        }

        val serviceExecParams = ServiceExecutionParameters(
            query = compileResult.document,
            context = executionInput.context,
            graphQLContext = executionInput.graphQLContext,
            executionId = executionInput.executionId ?: executionIdProvider.provide(executionInput),
            variables = compileResult.variables,
            operationDefinition = compileResult.document.definitions.singleOfType(),
            serviceContext = executionContext.getContextForService(service).await(),
            hydrationDetails = executionHydrationDetails,
            executableNormalizedField = transformedQuery,
        )

        val serviceExecResult = try {
            service.serviceExecution
                .execute(serviceExecParams)
                .asDeferred()
                .await()
        } catch (e: Exception) {
            val errorMessage = "An exception occurred invoking the service '${service.name}'"
            val errorMessageNotSafe = "$errorMessage: ${e.message}"
            val executionId = serviceExecParams.executionId.toString()

            instrumentation.onError(
                NadelInstrumentationOnErrorParameters(
                    message = errorMessage,
                    exception = e,
                    instrumentationState = executionContext.instrumentationState,
                    errorType = ServiceExecutionError,
                    errorData = ErrorData.ServiceExecutionErrorData(
                        executionId = executionId,
                        serviceName = service.name
                    )
                )
            )

            newServiceExecutionResult(
                errors = mutableListOf(
                    newGraphQLError(
                        message = errorMessageNotSafe, // End user can receive not safe message
                        errorType = ErrorType.DataFetchingException,
                        extensions = mutableMapOf(
                            "executionId" to executionId,
                        ),
                    ).toSpecification(),
                ),
            )
        }

        return serviceExecResult.copy(
            data = serviceExecResult.data.let { data ->
                data.takeIf { transformedQuery.resultKey in data }
                    ?: mutableMapOf(transformedQuery.resultKey to null)
            },
        )
    }

    private fun getDocumentVariablePredicate(hints: NadelExecutionHints, service: Service): VariablePredicate {
        return if (hints.allDocumentVariablesHint.invoke(service)) {
            DocumentPredicates.allVariablesPredicate
        } else {
            DocumentPredicates.jsonPredicate
        }
    }

    private fun getOperationName(service: Service, executionContext: NadelExecutionContext): String? {
        val originalOperationName = executionContext.query.operationName
        return if (executionContext.hints.legacyOperationNames(service)) {
            return OperationNameUtil.getLegacyOperationName(service.name, originalOperationName)
        } else {
            originalOperationName
        }
    }

    private suspend fun transformQuery(
        service: Service,
        executionContext: NadelExecutionContext,
        executionPlan: NadelExecutionPlan,
        field: ExecutableNormalizedField,
    ): NadelQueryTransformer.TransformResult {
        return NadelQueryTransformer.transformQuery(
            overallExecutionBlueprint,
            service,
            executionContext,
            executionPlan,
            field,
        )
    }
}
