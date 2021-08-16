package graphql.nadel

import graphql.ErrorType
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl.newExecutionResult
import graphql.GraphQLError
import graphql.execution.ResultPath
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprintFactory
import graphql.nadel.enginekt.defer.DeferSupport
import graphql.nadel.enginekt.defer.DeferredExecutionResultImpl
import graphql.nadel.enginekt.defer.ExecutionDeferredCall
import graphql.nadel.enginekt.defer.ServiceDeferredCall
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.plan.NadelExecutionPlanFactory
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.query.NadelFieldToService
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultTransformer
import graphql.nadel.enginekt.util.copy
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.fold
import graphql.nadel.enginekt.util.getInstrumentationContext
import graphql.nadel.enginekt.util.getOperationKind
import graphql.nadel.enginekt.util.mergeResults
import graphql.nadel.enginekt.util.newExecutionResult
import graphql.nadel.enginekt.util.newGraphQLError
import graphql.nadel.enginekt.util.newServiceExecutionResult
import graphql.nadel.enginekt.util.provide
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.util.ErrorUtil
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperationFactory
import graphql.normalized.ExecutableNormalizedOperationToAstCompiler.compileToDocument
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

class NextgenEngine @JvmOverloads constructor(
    nadel: Nadel,
    transforms: List<NadelTransform<out Any>> = emptyList(),
) : NadelExecutionEngine {
    private val services: Map<String, Service> = nadel.services.strictAssociateBy { it.name }
    private val overallSchema = nadel.overallSchema
    private val serviceExecutionHooks: ServiceExecutionHooks = nadel.serviceExecutionHooks
    private val overallExecutionBlueprint = NadelExecutionBlueprintFactory.create(
        overallSchema = nadel.overallSchema,
        services = nadel.services,
    )
    private val executionPlanner = NadelExecutionPlanFactory.create(
        executionBlueprint = overallExecutionBlueprint,
        engine = this,
        transforms = transforms
    )
    private val queryTransformer = NadelQueryTransformer.create(
        executionBlueprint = overallExecutionBlueprint,
    )
    private val resultTransformer = NadelResultTransformer(overallExecutionBlueprint)
    private val instrumentation = nadel.instrumentation
    private val fieldToService = NadelFieldToService(overallExecutionBlueprint)
    private val executionIdProvider = nadel.executionIdProvider

    override fun execute(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?,
        nadelExecutionParams: NadelExecutionParams,
    ): CompletableFuture<ExecutionResult> {
        return GlobalScope.async {
            executeCoroutine(executionInput, queryDocument, instrumentationState)
        }.asCompletableFuture()
    }

    private suspend fun executeCoroutine(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?,
    ): ExecutionResult {
        val query = try {
            ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables(
                overallExecutionBlueprint.schema,
                queryDocument,
                executionInput.operationName,
                executionInput.variables,
            )
        } catch (e: Throwable) {
            when (e) {
                is GraphQLError -> return newExecutionResult(error = e)
                else -> throw e
            }
        }

        val executionContext = NadelExecutionContext(
            executionInput = executionInput,
            hooks = serviceExecutionHooks,
            normalizedOperation = query
        )
        val instrumentationContext = getInstrumentationContext(query, queryDocument, executionInput, overallSchema, instrumentation, instrumentationState)

        // TODO: determine what to do with NQ
        // instrumentation.beginExecute(NadelInstrumentationExecuteOperationParameters(query))


        val result: ExecutionResult = try {
            mergeResults(
                coroutineScope {
                    fieldToService.getServicesForTopLevelFields(query)
                        .map { (field, service) ->
                            async {
                                try {
                                    val deferLabel = DeferSupport.getDeferLabel(query, field)
                                    if (deferLabel != null) {
                                        val call = ExecutionDeferredCall(
                                            path = ResultPath.rootPath(),
                                            label = deferLabel,
                                            call = { executeTopLevelField(field, service, executionContext) }
                                        )
                                        executionContext.deferSupport.enqueue(call)
                                        null
                                    } else {
                                        executeTopLevelField(field, service, executionContext)
                                    }

                                } catch (e: Throwable) {
                                    when (e) {
                                        is GraphQLError -> newExecutionResult(error = e)
                                        else -> throw e
                                    }
                                }
                            }
                        }
                }
                    .also { executionContext.deferSupport.startDeferredCalls() }
                    .awaitAll()
                    .filterNotNull()
            )
        } catch (e: Throwable) {
            instrumentationContext.onCompleted(null, e)
            throw e
        }
        instrumentationContext.onCompleted(result, null)

        return deferSupport(executionContext, result)
    }

    private fun deferSupport(
        executionContext: NadelExecutionContext,
        result: ExecutionResult
    ): ExecutionResult {
        val deferSupport: DeferSupport = executionContext.deferSupport
        if (deferSupport.isDeferDetected()) {
            // we start the rest of the query now to maximize throughput.  We have the initial important results
            // and now we can start the rest of the calls as early as possible (even before some one subscribes)
            val publisher = deferSupport.publisher
            val executionResultImpl = newExecutionResult().from(result)
                .addExtension("GRAPHQL_DEFERRED", publisher)
                .build()

            return DeferredExecutionResultImpl.newFirstExecutionResult(executionResultImpl)
        }

        return result
    }


    private suspend fun executeTopLevelField(
        topLevelField: ExecutableNormalizedField,
        service: Service,
        executionContext: NadelExecutionContext,
    ): ExecutionResult {
        val executionPlan = executionPlanner.create(executionContext, services, service, topLevelField)
        val queryTransform = queryTransformer.transformQuery(
            executionContext,
            service,
            topLevelField,
            executionPlan,
        )
        val transformedQuery = queryTransform.result.single()

        val result = executeService(service, transformedQuery, executionContext)
        val transformedResult = resultTransformer.transform(
            executionContext = executionContext,
            executionPlan = executionPlan,
            artificialFields = queryTransform.artificialFields,
            overallToUnderlyingFields = queryTransform.overallToUnderlyingFields,
            service = service,
            result = result,
        )

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
        hydratedField: ExecutableNormalizedField,
    ): ServiceExecutionResult? {
        val deferLabel = DeferSupport.getDeferLabel(executionContext.normalizedOperation, hydratedField)

        if (deferLabel != null) {
            val call = ServiceDeferredCall(
                // TODO: This won't be correct if hydration is on list item NadelQueryPath don't know items indexes
                // TODO: need to use the field name of the original query, not the hydration query
                path = ResultPath.fromList(hydratedField.queryPath.dropLast(1).segments),
                label = deferLabel,
                call = {
                    executeHydrationImpl(
                        service = service,
                        topLevelField = topLevelField,
                        pathToActorField = pathToActorField,
                        executionContext = executionContext
                    )
                }
            )
            executionContext.deferSupport.enqueue(call)
            return null
        }

        return executeHydrationImpl(
            service = service,
            topLevelField = topLevelField,
            pathToActorField = pathToActorField,
            executionContext = executionContext
        )
    }

    private suspend fun executeHydrationImpl(
        service: Service,
        topLevelField: ExecutableNormalizedField,
        pathToActorField: NadelQueryPath,
        executionContext: NadelExecutionContext,
    ): ServiceExecutionResult {


        val actorField = fold(initial = topLevelField, count = pathToActorField.segments.size - 1) {
            it.children.single()
        }

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
                queryTransformer.transformQuery(executionContext, service, field = childField, executionPlan)
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

        // Get to the top level field again using .parent N times on the new actor field
        val transformedQuery: ExecutableNormalizedField = fold(
            initial = actorFieldWithTransformedChildren,
            count = pathToActorField.segments.size - 1,
        ) {
            it.parent ?: error("No parent")
        }

        val result = executeService(
            service,
            transformedQuery,
            executionContext,
            isHydration = true,
        )
        return resultTransformer.transform(
            executionContext = executionContext,
            executionPlan = executionPlan,
            artificialFields = artificialFields,
            overallToUnderlyingFields = overallToUnderlyingFields,
            service = service,
            result = result,
        )
    }

    private suspend fun executeService(
        service: Service,
        transformedQuery: ExecutableNormalizedField,
        executionContext: NadelExecutionContext,
        isHydration: Boolean = false,
    ): ServiceExecutionResult {
        val executionInput = executionContext.executionInput
        val document: Document = compileToDocument(
            transformedQuery.getOperationKind(overallSchema),
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
            .hydrationCall(isHydration)
            .build()

        val serviceExecResult = try {
            service.serviceExecution
                .execute(serviceExecParams)
                .asDeferred()
                .await()
        } catch (e: Exception) {
            newServiceExecutionResult(
                errors = mutableListOf(
                    newGraphQLError(
                        message = "An exception occurred invoking the service '${service.name}': ${e.message}",
                        errorType = ErrorType.DataFetchingException,
                        extensions = mutableMapOf(
                            "executionId" to serviceExecParams.executionId.toString(),
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

    companion object {
        @JvmStatic
        fun newNadel(): Nadel.Builder {
            return Nadel.Builder().engineFactory(::NextgenEngine)
        }
    }
}
