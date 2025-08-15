package graphql.nadel

import graphql.ErrorType
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.execution.ExecutionIdProvider
import graphql.execution.UnknownOperationException
import graphql.execution.instrumentation.InstrumentationState
import graphql.incremental.DeferPayload
import graphql.incremental.IncrementalExecutionResultImpl
import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelIncrementalResultSupport
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.IntrospectionService
import graphql.nadel.engine.blueprint.NadelIntrospectionRunnerFactory
import graphql.nadel.engine.document.DocumentPredicates
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.plan.NadelExecutionPlanFactory
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.query.DynamicServiceResolution
import graphql.nadel.engine.transform.query.NadelFieldToService
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.query.NadelQueryTransformer.Companion.transformQuery
import graphql.nadel.engine.transform.result.NadelResultTransformer
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.beginExecute
import graphql.nadel.engine.util.compileToDocument
import graphql.nadel.engine.util.getOperationDefinitionOrNull
import graphql.nadel.engine.util.getOperationKind
import graphql.nadel.engine.util.newExecutionResult
import graphql.nadel.engine.util.newGraphQLError
import graphql.nadel.engine.util.newServiceExecutionErrorResult
import graphql.nadel.engine.util.newServiceExecutionResult
import graphql.nadel.engine.util.provide
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.hooks.createOperationExecutionContext
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.ErrorData
import graphql.nadel.instrumentation.parameters.ErrorType.ServiceExecutionError
import graphql.nadel.instrumentation.parameters.NadelInstrumentationIsTimingEnabledParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnErrorParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep.Companion.DocumentCompilation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep
import graphql.nadel.instrumentation.parameters.child
import graphql.nadel.result.NadelResultMerger
import graphql.nadel.result.NadelResultTracker
import graphql.nadel.time.NadelInternalLatencyTracker
import graphql.nadel.util.NamespacedUtil.isNamespacedFieldLike
import graphql.nadel.util.OperationNameUtil
import graphql.nadel.validation.NadelSchemaValidation
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.CompletableFuture
import graphql.normalized.ExecutableNormalizedOperationFactory.Options.defaultOptions as executableNormalizedOperationFactoryOptions

internal class NextgenEngine(
    private val engineSchema: GraphQLSchema,
    private val querySchema: GraphQLSchema,
    private val instrumentation: NadelInstrumentation,
    private val executionHooks: NadelExecutionHooks,
    private val executionIdProvider: ExecutionIdProvider,
    maxQueryDepth: Int,
    maxFieldCount: Int,
    services: List<Service>,
    transforms: List<NadelTransform<*, *>>,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
    nadelValidation: NadelSchemaValidation,
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services: Map<String, Service> = services.strictAssociateBy { it.name }
    private val engineSchemaIntrospectionService = IntrospectionService(engineSchema, introspectionRunnerFactory)
    private val overallExecutionBlueprint = nadelValidation
        .validateAndGenerateBlueprint(NadelSchemas(engineSchema, services))
    private val executionPlanner = NadelExecutionPlanFactory.create(
        engine = this,
        transforms = transforms,
        executionHooks = executionHooks,
    )
    private val resultTransformer = NadelResultTransformer()
    private val dynamicServiceResolution = DynamicServiceResolution(
        engineSchema = engineSchema,
        executionHooks = executionHooks,
        services = services,
    )
    private val fieldToService = NadelFieldToService(
        querySchema = querySchema,
        overallExecutionBlueprint = overallExecutionBlueprint,
        introspectionRunnerFactory = introspectionRunnerFactory,
        dynamicServiceResolution = dynamicServiceResolution,
        services = this.services,
    )
    private val baseParseOptions = executableNormalizedOperationFactoryOptions()
        .maxChildrenDepth(maxQueryDepth)
        .maxFieldsCount(maxFieldCount)

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
                nadelExecutionParams.executionHints,
                nadelExecutionParams.latencyTracker,
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
        latencyTracker: NadelInternalLatencyTracker,
    ): ExecutionResult = supervisorScope {
        try {
            val operationDefinition = queryDocument.getOperationDefinitionOrNull(executionInput.operationName)
                ?: throw UnknownOperationException("Must provide operation name if query contains multiple operations")

            val timer = makeTimer(operationDefinition, executionInput, latencyTracker, instrumentationState)

            val operationParseOptions = baseParseOptions
                .deferSupport(executionHints.deferSupport.invoke())

            val operation = timer.time(step = RootStep.ExecutableOperationParsing) {
                createExecutableNormalizedOperationWithRawVariables(
                    querySchema,
                    queryDocument,
                    executionInput.operationName,
                    executionInput.rawVariables,
                    operationParseOptions
                        .graphQLContext(executionInput.graphQLContext),
                )
            }

            val incrementalResultSupport = NadelIncrementalResultSupport(operation)
            val resultTracker = NadelResultTracker()
            val executionContext = NadelExecutionContext(
                overallExecutionBlueprint,
                executionInput,
                operation,
                executionHooks,
                executionHints,
                instrumentationState,
                timer,
                incrementalResultSupport,
                resultTracker,
                executionCoroutine = this@supervisorScope,
            )

            val beginExecuteContext = instrumentation.beginExecute(
                operation,
                queryDocument,
                operationDefinition,
                executionInput,
                engineSchema,
                instrumentationState,
            )

            val result: ExecutionResult = try {
                val fields = fieldToService.getServicesForTopLevelFields(operation, executionHints)
                val results = coroutineScope {
                    fields
                        .map { (field, service) ->
                            async {
                                try {
                                    val resolvedService = fieldToService.resolveDynamicService(field, service)
                                    executeTopLevelField(
                                        executionContext = executionContext,
                                        service = resolvedService,
                                        topLevelField = field,
                                        hydrationDetails = null,
                                        isPartitionedCall = false,
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
            incrementalResultSupport.onInitialResultComplete()

            // todo: maybe pass in the incremental version that's built below into here
            resultTracker.complete(result)

            if (incrementalResultSupport.hasDeferredResults()) {
                IncrementalExecutionResultImpl.Builder()
                    .from(result)
                    .incrementalItemPublisher(incrementalResultSupport.resultFlow().asPublisher())
                    .build()
            } else {
                result
            }
        } catch (e: Throwable) {
            when (e) {
                is GraphQLError -> newExecutionResult(error = e)
                else -> throw e
            }
        }
    }

    internal suspend fun executeHydration(
        topLevelField: ExecutableNormalizedField,
        service: Service,
        executionContext: NadelExecutionContext,
        hydrationDetails: NadelOperationExecutionHydrationDetails,
    ): ServiceExecutionResult {
        return try {
            executeTopLevelField(
                executionContext = executionContext,
                service = service,
                topLevelField = topLevelField,
                hydrationDetails = hydrationDetails,
                isPartitionedCall = false,
            )
        } catch (e: Exception) {
            when (e) {
                is GraphQLError -> newServiceExecutionErrorResult(
                    field = topLevelField,
                    error = e,
                )
                else -> throw e
            }
        }
    }

    internal suspend fun executePartitionedCall(
        parentContext: NadelOperationExecutionContext,
        topLevelField: ExecutableNormalizedField,
    ): ServiceExecutionResult {
        return executeTopLevelField(
            executionContext = parentContext.executionContext,
            service = parentContext.service,
            topLevelField = topLevelField,
            hydrationDetails = parentContext.hydrationDetails,
            isPartitionedCall = true,
        )
    }

    private suspend fun executeTopLevelField(
        executionContext: NadelExecutionContext,
        service: Service,
        topLevelField: ExecutableNormalizedField,
        hydrationDetails: NadelOperationExecutionHydrationDetails?,
        isPartitionedCall: Boolean,
    ): ServiceExecutionResult {
        val operationExecutionContext = executionHooks.createOperationExecutionContext(
            executionContext = executionContext,
            service = service,
            topLevelField = topLevelField,
            hydrationDetails = hydrationDetails,
            isPartitionedCall = isPartitionedCall,
        )

        val timer = executionContext.timer
        val executionPlan = timer.time(step = RootStep.ExecutionPlanning) {
            executionPlanner.create(
                operationExecutionContext = operationExecutionContext,
                rootField = topLevelField,
            )
        }
        val queryTransform = timer.time(step = RootStep.QueryTransforming) {
            transformQuery(
                executionPlan = executionPlan,
                field = topLevelField
            )
        }
        val result: ServiceExecutionResult = timer.time(step = RootStep.ServiceExecution.child(service.name)) {
            executeService(
                service = service,
                topLevelFields = queryTransform.result,
                operationExecutionContext = operationExecutionContext,
            )
        }
        if (result is NadelIncrementalServiceExecutionResult) {
            executionContext.incrementalResultSupport.defer(
                result.incrementalItemPublisher
                    .asFlow()
                    .onEach { delayedIncrementalResult ->
                        // Transform
                        delayedIncrementalResult.incremental
                            ?.filterIsInstance<DeferPayload>()
                            ?.forEach { deferPayload ->
                                resultTransformer
                                    .transform(
                                        executionPlan = executionPlan,
                                        artificialFields = queryTransform.artificialFields,
                                        overallToUnderlyingFields = queryTransform.overallToUnderlyingFields,
                                        deferPayload = deferPayload,
                                    )
                            }
                    }
            )
        }
        val transformedResult: ServiceExecutionResult = when {
            topLevelField.name.startsWith("__") -> result
            else -> timer.time(step = RootStep.ResultTransforming) {
                resultTransformer.transform(
                    executionPlan = executionPlan,
                    artificialFields = queryTransform.artificialFields,
                    overallToUnderlyingFields = queryTransform.overallToUnderlyingFields,
                    result = result,
                )
            }
        }

        return transformedResult
    }

    private suspend fun executeService(
        service: Service,
        topLevelFields: List<ExecutableNormalizedField>,
        operationExecutionContext: NadelOperationExecutionContext,
    ): ServiceExecutionResult {
        val executionContext = operationExecutionContext.executionContext
        val timer = executionContext.timer
        val executionInput = executionContext.executionInput
        val hints = executionContext.hints

        val jsonPredicate: VariablePredicate = getDocumentVariablePredicate(hints, service)

        val compileResult = timer.time(step = DocumentCompilation) {
            compileToDocument(
                schema = service.underlyingSchema,
                operationKind = topLevelFields.first().getOperationKind(engineSchema),
                operationName = getOperationName(operationExecutionContext),
                topLevelFields = topLevelFields,
                variablePredicate = jsonPredicate,
                deferSupport = hints.deferSupport(),
            )
        }

        val serviceExecParams = ServiceExecutionParameters(
            query = compileResult.document,
            context = executionInput.context,
            graphQLContext = executionInput.graphQLContext,
            executionId = executionInput.executionId ?: executionIdProvider.provide(executionInput),
            variables = compileResult.variables,
            operationDefinition = compileResult.document.definitions.singleOfType(),
            operationExecutionContext = operationExecutionContext,
            // Prefer non __typename field first, otherwise we just get first
            executableNormalizedField = topLevelFields
                .asSequence()
                .filterNot {
                    it.fieldName == TypeNameMetaFieldDef.name
                }
                .firstOrNull() ?: topLevelFields.first(),
        )

        val serviceExecution = getServiceExecution(service, topLevelFields, hints)
        val serviceExecResult = try {
            serviceExecution.execute(serviceExecParams)
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

        val transformedData: MutableJsonMap = serviceExecResult.data
            .let { data ->
                // Ensures data always has root fields as keys
                topLevelFields
                    .asSequence()
                    .map {
                        it.resultKey
                    }
                    .associateWithTo(mutableMapOf()) { resultKey ->
                        data[resultKey]
                    }
            }

        return when (serviceExecResult) {
            is NadelServiceExecutionResultImpl -> serviceExecResult.copy(data = transformedData)
            is NadelIncrementalServiceExecutionResult -> serviceExecResult.copy(data = transformedData)
        }
    }

    private fun getServiceExecution(
        service: Service,
        topLevelFields: List<ExecutableNormalizedField>,
        hints: NadelExecutionHints,
    ): ServiceExecution {
        if (hints.shortCircuitEmptyQuery(service) && isOnlyTopLevelFieldTypename(topLevelFields, service)) {
            return engineSchemaIntrospectionService.serviceExecution
        }

        return service.serviceExecution
    }

    private fun isOnlyTopLevelFieldTypename(
        topLevelFields: List<ExecutableNormalizedField>,
        service: Service,
    ): Boolean {
        val topLevelField = topLevelFields.singleOrNull() ?: return false

        if (topLevelField.name == TypeNameMetaFieldDef.name) {
            return true
        }

        return isNamespacedFieldLike(service, topLevelField)
            && topLevelField.hasChildren()
            && topLevelField.children.all { it.name == TypeNameMetaFieldDef.name }
    }

    private fun getDocumentVariablePredicate(hints: NadelExecutionHints, service: Service): VariablePredicate {
        return if (hints.allDocumentVariablesHint.invoke(service)) {
            DocumentPredicates.allVariablesPredicate
        } else {
            DocumentPredicates.jsonPredicate
        }
    }

    private fun getOperationName(operationExecutionContext: NadelOperationExecutionContext): String? {
        val originalOperationName = operationExecutionContext.executionContext.query.operationName
        return if (executionContext.hints.legacyOperationNames(operationExecutionContext.service)) {
            return OperationNameUtil.getLegacyOperationName(service.name, originalOperationName)
        } else {
            originalOperationName
        }
    }

    private fun makeTimer(
        operationDefinition: OperationDefinition,
        executionInput: ExecutionInput,
        latencyTracker: NadelInternalLatencyTracker,
        instrumentationState: InstrumentationState?,
    ): NadelInstrumentationTimer {
        val isTimerEnabled = instrumentation.isTimingEnabled(
            params = NadelInstrumentationIsTimingEnabledParameters(
                instrumentationState = instrumentationState,
                context = executionInput.context,
                operationName = operationDefinition.name,
            ),
        )

        return NadelInstrumentationTimer(
            isEnabled = isTimerEnabled,
            ticker = latencyTracker::getInternalLatency,
            instrumentation = instrumentation,
            userContext = executionInput.context,
            instrumentationState = instrumentationState,
        )
    }
}
