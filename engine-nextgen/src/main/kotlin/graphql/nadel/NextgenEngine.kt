package graphql.nadel

import graphql.ErrorType
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl.newExecutionResult
import graphql.GraphQLError
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelDefaultIntrospectionRunner
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprintFactory
import graphql.nadel.enginekt.blueprint.NadelIntrospectionRunnerFactory
import graphql.nadel.enginekt.log.getLogger
import graphql.nadel.enginekt.log.getNotPrivacySafeLogger
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.plan.NadelExecutionPlanFactory
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.query.DynamicServiceResolution
import graphql.nadel.enginekt.transform.query.NadelFieldToService
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultTransformer
import graphql.nadel.enginekt.util.beginExecute
import graphql.nadel.enginekt.util.copy
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.fold
import graphql.nadel.enginekt.util.getOperationKind
import graphql.nadel.enginekt.util.mergeResults
import graphql.nadel.enginekt.util.newExecutionErrorResult
import graphql.nadel.enginekt.util.newExecutionResult
import graphql.nadel.enginekt.util.newGraphQLError
import graphql.nadel.enginekt.util.newServiceExecutionResult
import graphql.nadel.enginekt.util.provide
import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.toBuilder
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.util.ErrorUtil
import graphql.nadel.util.OperationNameUtil
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables
import graphql.normalized.ExecutableNormalizedOperationToAstCompiler.compileToDocument
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

class NextgenEngine @JvmOverloads constructor(
    nadel: Nadel,
    transforms: List<NadelTransform<out Any>> = emptyList(),
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory = NadelIntrospectionRunnerFactory(::NadelDefaultIntrospectionRunner),
) : NadelExecutionEngine {
    private val logNotSafe = getNotPrivacySafeLogger<NextgenEngine>()
    private val log = getLogger<NextgenEngine>()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val services: Map<String, Service> = nadel.services.strictAssociateBy { it.name }
    private val publicOverallSchema = nadel.publicOverallSchema
    private val serviceExecutionHooks: ServiceExecutionHooks = nadel.serviceExecutionHooks
    private val overallExecutionBlueprint = NadelExecutionBlueprintFactory.create(
        privateOverallSchema = nadel.privateOverallSchema,
        publicOverallSchema = nadel.publicOverallSchema,
        services = nadel.services,
    )
    private val executionPlanner = NadelExecutionPlanFactory.create(
        executionBlueprint = overallExecutionBlueprint,
        engine = this,
        transforms = transforms,
    )
    private val resultTransformer = NadelResultTransformer(overallExecutionBlueprint)
    private val instrumentation = nadel.instrumentation
    private val dynamicServiceResolution = DynamicServiceResolution(
        publicOverallSchema = publicOverallSchema,
        serviceExecutionHooks = serviceExecutionHooks,
        services = services.values
    )
    private val fieldToService = NadelFieldToService(
        overallExecutionBlueprint = overallExecutionBlueprint,
        introspectionRunnerFactory = introspectionRunnerFactory,
        dynamicServiceResolution = dynamicServiceResolution,
        services,
    )
    private val executionIdProvider = nadel.executionIdProvider

    override fun execute(
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

    override fun close() {
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
            val query = createExecutableNormalizedOperationWithRawVariables(
                overallExecutionBlueprint.publicSchema,
                queryDocument,
                executionInput.operationName,
                executionInput.variables,
            )

            val executionContext = NadelExecutionContext(executionInput, query, serviceExecutionHooks, executionHints)
            val beginExecuteContext = instrumentation.beginExecute(
                query,
                queryDocument,
                executionInput,
                publicOverallSchema,
                instrumentationState,
            )

            val result: ExecutionResult = try {
                mergeResults(
                    coroutineScope {
                        fieldToService.getServicesForTopLevelFields(query)
                            .map { (field, service) ->
                                async {
                                    try {
                                        val resolvedService = fieldToService.resolveDynamicService(field, service)
                                        executeTopLevelField(field, resolvedService, executionContext)
                                    } catch (e: Throwable) {
                                        when (e) {
                                            is GraphQLError -> newExecutionErrorResult(field, error = e)
                                            else -> throw e
                                        }
                                    }
                                }
                            }
                    }.awaitAll()
                )
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

    private suspend fun executeTopLevelField(
        topLevelField: ExecutableNormalizedField,
        service: Service,
        executionContext: NadelExecutionContext,
    ): ExecutionResult {
        val executionPlan = executionPlanner.create(executionContext, services, service, topLevelField)
        val queryTransform = transformQuery(service, executionContext, executionPlan, topLevelField)
        val transformedQuery = queryTransform.result.single()
        val result: ServiceExecutionResult = executeService(service, transformedQuery, executionContext)
        val transformedResult: ServiceExecutionResult = when {
            topLevelField.name.startsWith("__") -> result
            else -> resultTransformer.transform(
                executionContext = executionContext,
                executionPlan = executionPlan,
                artificialFields = queryTransform.artificialFields,
                overallToUnderlyingFields = queryTransform.overallToUnderlyingFields,
                service = service,
                result = result,
            )
        }

        @Suppress("UNCHECKED_CAST")
        return newExecutionResult()
            .data(transformedResult.data)
            .errors(ErrorUtil.createGraphQlErrorsFromRawErrors(transformedResult.errors))
            .extensions(transformedResult.extensions as Map<Any, Any>)
            .build()
    }

    internal suspend fun executeHydration(
        service: Service,
        topLevelField: ExecutableNormalizedField,
        pathToActorField: NadelQueryPath,
        executionContext: NadelExecutionContext,
        serviceHydrationDetails: ServiceExecutionHydrationDetails,
    ): ServiceExecutionResult {
        val actorField = fold(initial = topLevelField, count = pathToActorField.segments.size - 1) {
            it.children.single()
        }

        val (transformResult, executionPlan) = when (executionContext.hints.transformsOnHydrationFields) {
            true -> transformActorFieldNew(service, executionContext, actorField)
            else -> transformActorField(service, executionContext, actorField)
        }

        // Get to the top level field again using .parent N times on the new actor field
        val transformedQuery: ExecutableNormalizedField = fold(
            initial = transformResult.result.single(),
            count = pathToActorField.segments.size - 1,
        ) {
            it.parent ?: error("No parent")
        }

        val result = executeService(
            service,
            transformedQuery,
            executionContext,
            serviceHydrationDetails,
        )

        return resultTransformer.transform(
            executionContext = executionContext,
            executionPlan = executionPlan,
            artificialFields = transformResult.artificialFields,
            overallToUnderlyingFields = transformResult.overallToUnderlyingFields,
            service = service,
            result = result,
        )
    }

    private suspend fun transformActorField(
        service: Service,
        executionContext: NadelExecutionContext,
        actorField: ExecutableNormalizedField,
    ): Pair<NadelQueryTransformer.TransformResult, NadelExecutionPlan> {

        // Creates N plans for the children then merges them together into one big plan
        val executionPlan = actorField.children.map {
            executionPlanner.create(executionContext, services, service, rootField = it)
        }.reduce(NadelExecutionPlan::merge)

        val artificialFields = mutableListOf<ExecutableNormalizedField>()
        val overallToUnderlyingFields = mutableMapOf<ExecutableNormalizedField, List<ExecutableNormalizedField>>()

        // Transform the children of the actor field
        // The actor field itself is already transformed
        val actorFieldWithTransformedChildren = actorField.copyWithChildren(
            actorField.children.flatMap { childField ->
                transformQuery(service, executionContext, executionPlan, field = childField)
                    .let { result ->
                        artificialFields.addAll(result.artificialFields)
                        overallToUnderlyingFields.also { map ->
                            val sizeBefore = map.size
                            map.putAll(result.overallToUnderlyingFields)
                            require(map.size == sizeBefore + result.overallToUnderlyingFields.size)
                        }
                        result.result
                    }
            },
        )

        return Pair(
            NadelQueryTransformer.TransformResult(
                result = listOf(actorFieldWithTransformedChildren),
                artificialFields,
                overallToUnderlyingFields
            ), executionPlan
        )
    }

    private suspend fun transformActorFieldNew(
        service: Service,
        executionContext: NadelExecutionContext,
        actorField: ExecutableNormalizedField,
    ): Pair<NadelQueryTransformer.TransformResult, NadelExecutionPlan> {
        val executionPlan = executionPlanner.create(executionContext, services, service, rootField = actorField)

        val queryTransform = transformQuery(service, executionContext, executionPlan, actorField)

        // Fix parent of the actor field
        if (actorField.parent != null) {
            val fixedParent = actorField.parent.toBuilder().children(queryTransform.result).build()
            val queryTransformResult = queryTransform.result.single()
            queryTransformResult.replaceParent(fixedParent)
        }

        return Pair(queryTransform, executionPlan)
    }

    private suspend fun executeService(
        service: Service,
        transformedQuery: ExecutableNormalizedField,
        executionContext: NadelExecutionContext,
        executionHydrationDetails: ServiceExecutionHydrationDetails? = null,
    ): ServiceExecutionResult {
        val executionInput = executionContext.executionInput
        val document: Document = compileToDocument(
            service.underlyingSchema,
            transformedQuery.getOperationKind(publicOverallSchema),
            getOperationName(service, executionContext),
            listOf(transformedQuery),
        )

        val serviceExecParams = newServiceExecutionParameters()
            .query(document)
            .context(executionInput.context)
            .executionId(executionInput.executionId ?: executionIdProvider.provide(executionInput))
            .cacheControl(executionInput.cacheControl)
            .variables(emptyMap())
            .fragments(emptyMap())
            .operationDefinition(document.definitions.singleOfType())
            .serviceContext(executionContext.getContextForService(service).await())
            .executionHydrationDetails(executionHydrationDetails)
            .build()

        val serviceExecResult = try {
            service.serviceExecution
                .execute(serviceExecParams)
                .asDeferred()
                .await()
        } catch (e: Exception) {
            val errorMessage = "An exception occurred invoking the service '${service.name}'"
            val errorMessageNotSafe = "$errorMessage: ${e.message}"
            val executionId = serviceExecParams.executionId.toString()
            logNotSafe.error("$errorMessageNotSafe. Execution ID '$executionId'", e)
            log.error("$errorMessage. Execution ID '$executionId'", e)

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
                data?.takeIf { transformedQuery.resultKey in data }
                    ?: mutableMapOf(transformedQuery.resultKey to null)
            },
        )
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

    companion object {
        @JvmStatic
        fun newNadel(): Nadel.Builder {
            return Nadel.Builder().engineFactory(::NextgenEngine)
        }
    }
}
