package graphql.nadel

import graphql.ErrorType
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.execution.ExecutionIdProvider
import graphql.execution.instrumentation.InstrumentationState
import graphql.incremental.DeferPayload
import graphql.incremental.IncrementalExecutionResultImpl
import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.language.Document
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelIncrementalResultSupport
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.IntrospectionService
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
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.beginExecute
import graphql.nadel.engine.util.compileToDocument
import graphql.nadel.engine.util.getOperationKind
import graphql.nadel.engine.util.newExecutionResult
import graphql.nadel.engine.util.newGraphQLError
import graphql.nadel.engine.util.newServiceExecutionErrorResult
import graphql.nadel.engine.util.newServiceExecutionResult
import graphql.nadel.engine.util.provide
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.hooks.createServiceExecutionContext
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.ErrorData
import graphql.nadel.instrumentation.parameters.ErrorType.ServiceExecutionError
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnErrorParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep.Companion.DocumentCompilation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep
import graphql.nadel.instrumentation.parameters.child
import graphql.nadel.result.NadelResultMerger
import graphql.nadel.result.NadelResultTracker
import graphql.nadel.util.OperationNameUtil
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables
import graphql.normalized.VariablePredicate
import graphql.schema.GraphQLObjectType
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
import kotlinx.coroutines.future.await
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
    transforms: List<NadelTransform<out Any>> = emptyList(),
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory = NadelIntrospectionRunnerFactory(::NadelDefaultIntrospectionRunner),
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services: Map<String, Service> = services.strictAssociateBy { it.name }
    private val engineSchemaIntrospectionService = IntrospectionService(engineSchema, introspectionRunnerFactory)
    private val overallExecutionBlueprint = NadelExecutionBlueprintFactory.create(
        engineSchema = engineSchema,
        services = services,
    )
    private val executionPlanner = NadelExecutionPlanFactory.create(
        executionBlueprint = overallExecutionBlueprint,
        engine = this,
        transforms = transforms,
        executionHooks = executionHooks,
    )
    private val resultTransformer = NadelResultTransformer(overallExecutionBlueprint)
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
    ): ExecutionResult = supervisorScope {
        try {
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = executionInput.context,
                instrumentationState,
            )

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
                executionInput,
                operation,
                executionHooks,
                executionHints,
                instrumentationState,
                timer,
                incrementalResultSupport,
                resultTracker,
                executionCoroutine = this
            )

            val beginExecuteContext = instrumentation.beginExecute(
                operation,
                queryDocument,
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
        hydrationDetails: ServiceExecutionHydrationDetails,
    ): ServiceExecutionResult {
        return executeTopLevelField(
            topLevelField = topLevelField,
            service = service,
            executionContext = executionContext.copy(
                hydrationDetails = hydrationDetails,
            ),
        )
    }

    internal suspend fun executePartitionedCall(
        topLevelField: ExecutableNormalizedField,
        service: Service,
        executionContext: NadelExecutionContext,
    ): ServiceExecutionResult {
        return executeTopLevelField(
            topLevelField = topLevelField,
            service = service,
            executionContext = executionContext.copy(
                isPartitionedCall = true,
            ),
        )
    }

    private suspend fun executeTopLevelField(
        topLevelField: ExecutableNormalizedField,
        service: Service,
        executionContext: NadelExecutionContext,
    ): ServiceExecutionResult {
        val serviceExecutionContext = executionHooks.createServiceExecutionContext(service)

        val timer = executionContext.timer
        val executionPlan = timer.time(step = RootStep.ExecutionPlanning) {
            executionPlanner.create(
                executionContext = executionContext,
                serviceExecutionContext = serviceExecutionContext,
                services = services,
                service = service,
                rootField = topLevelField,
                serviceHydrationDetails = executionContext.hydrationDetails,
            )
        }
        val queryTransform = timer.time(step = RootStep.QueryTransforming) {
            transformQuery(
                service = service,
                executionContext = executionContext,
                serviceExecutionContext = serviceExecutionContext,
                executionPlan = executionPlan,
                field = topLevelField
            )
        }
        val result: ServiceExecutionResult = timer.time(step = RootStep.ServiceExecution.child(service.name)) {
            executeService(
                service = service,
                topLevelFields = queryTransform.result,
                executionContext = executionContext,
                serviceExecutionContext = serviceExecutionContext,
                executionHydrationDetails = executionContext.hydrationDetails,
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
                                        executionContext = executionContext,
                                        serviceExecutionContext = serviceExecutionContext,
                                        executionPlan = executionPlan,
                                        artificialFields = queryTransform.artificialFields,
                                        overallToUnderlyingFields = queryTransform.overallToUnderlyingFields,
                                        service = service,
                                        result = result,
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
                    executionContext = executionContext,
                    serviceExecutionContext = serviceExecutionContext,
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
        topLevelFields: List<ExecutableNormalizedField>,
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionHydrationDetails: ServiceExecutionHydrationDetails? = null,
    ): ServiceExecutionResult {
        val timer = executionContext.timer

        val executionInput = executionContext.executionInput

        val jsonPredicate: VariablePredicate = getDocumentVariablePredicate(executionContext.hints, service)

        val compileResult = timer.time(step = DocumentCompilation) {
            compileToDocument(
                schema = service.underlyingSchema,
                operationKind = topLevelFields.first().getOperationKind(engineSchema),
                operationName = getOperationName(service, executionContext),
                topLevelFields = topLevelFields,
                variablePredicate = jsonPredicate,
                deferSupport = executionContext.hints.deferSupport(),
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
            serviceExecutionContext = serviceExecutionContext,
            hydrationDetails = executionHydrationDetails,
            // Prefer non __typename field first, otherwise we just get first
            executableNormalizedField = topLevelFields
                .asSequence()
                .filterNot {
                    it.fieldName == TypeNameMetaFieldDef.name
                }
                .firstOrNull() ?: topLevelFields.first(),
        )

        val serviceExecution = getServiceExecution(service, topLevelFields, executionContext.hints)
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

        if (topLevelField.fieldName == TypeNameMetaFieldDef.name) {
            return true
        }
        val operationType = service.underlyingSchema.getTypeAs<GraphQLObjectType>(topLevelField.singleObjectTypeName)
        val topLevelFieldDefinition = operationType.getField(topLevelField.name)
        val isNamespacedLike = topLevelFieldDefinition?.arguments?.isEmpty() == true
            && topLevelFieldDefinition.type is GraphQLObjectType
        return isNamespacedLike &&
            topLevelField.hasChildren() &&
            topLevelField.children.all { it.name == TypeNameMetaFieldDef.name }
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
        serviceExecutionContext: NadelServiceExecutionContext,
        executionPlan: NadelExecutionPlan,
        field: ExecutableNormalizedField,
    ): NadelQueryTransformer.TransformResult {
        return NadelQueryTransformer.transformQuery(
            overallExecutionBlueprint,
            service,
            executionContext,
            serviceExecutionContext,
            executionPlan,
            field,
        )
    }
}
