package graphql.nadel

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl.newExecutionResult
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.language.NodeUtil
import graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprintFactory
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.plan.NadelExecutionPlanFactory
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.query.NadelFieldToService
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.transform.result.NadelResultTransformer
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.fold
import graphql.nadel.enginekt.util.mergeResults
import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.hooks.CreateServiceContextParams
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

class NextgenEngine @JvmOverloads constructor(nadel: Nadel, transforms: List<NadelTransform<out Any>> = emptyList()) : NadelExecutionEngine {
    private val services: Map<String, Service> = nadel.services.strictAssociateBy { it.name }
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
        val query = ExecutableNormalizedOperationFactory.createExecutableNormalizedOperationWithRawVariables(
            overallExecutionBlueprint.schema,
            queryDocument,
            executionInput.operationName,
            executionInput.variables,
        )

        val executionContext = NadelExecutionContext(executionInput, serviceExecutionHooks)

        // TODO: determine what to do with NQ
        // instrumentation.beginExecute(NadelInstrumentationExecuteOperationParameters(query))

        val results = coroutineScope {
            fieldToService.getServicesForTopLevelFields(query)
                .map { (field, service) ->
                    async {
                        executeTopLevelField(field, service, executionContext)
                    }
                }
        }.awaitAll()

        return mergeResults(results)
    }

    private suspend fun executeTopLevelField(
        topLevelField: ExecutableNormalizedField,
        service: Service,
        executionContext: NadelExecutionContext
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
        pathToActorField: QueryPath,
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

        val result = executeService(service, transformedQuery, executionContext)
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
    ): ServiceExecutionResult {
        val document: Document = compileToDocument(listOf(transformedQuery))
        val executionInput = executionContext.executionInput

        return service.serviceExecution.execute(
            newServiceExecutionParameters()
                .query(document)
                .context(executionInput.context)
                .executionId(executionInput.executionId)
                .cacheControl(executionInput.cacheControl)
                .variables(emptyMap())
                .fragments(emptyMap())
                .operationDefinition(document.definitions.singleOfType())
                .serviceContext(executionContext.getContextForService(service).await())
                .hydrationCall(false)
                .build()
        ).asDeferred().await()
    }

    private fun getOperationKind(queryDocument: Document, operationName: String?): OperationKind {
        val operation = NodeUtil.getOperation(queryDocument, operationName)
        return OperationKind.fromAst(operation.operationDefinition.operation)
    }

    companion object {
        @JvmStatic
        fun newNadel(): Nadel.Builder {
            return Nadel.Builder().engineFactory(::NextgenEngine)
        }
    }
}
