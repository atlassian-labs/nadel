package graphql.nadel

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.language.NodeUtil
import graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprintFactory
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.plan.NadelExecutionPlanFactory
import graphql.nadel.enginekt.schema.NadelFieldInfos
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultTransformer
import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.util.ErrorUtil
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQueryFactory
import graphql.normalized.NormalizedQueryToAstCompiler.compileToDocument
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import java.util.concurrent.CompletableFuture

class NextgenEngine(nadel: Nadel) : NadelExecutionEngine {
    private val overallSchema = nadel.overallSchema
    private val services = nadel.services.strictAssociateBy { it.name }
    private val fieldInfos = NadelFieldInfos.create(nadel.services)
    private val executionBlueprint = NadelExecutionBlueprintFactory.create(overallSchema, nadel.services)
    private val executionPlanner = NadelExecutionPlanFactory.create(executionBlueprint, nadel.overallSchema, this)
    private val queryTransformer = NadelQueryTransformer.create(nadel.overallSchema)
    private val resultTransformer = NadelResultTransformer(nadel.overallSchema)
    private val instrumentation = nadel.instrumentation

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
        val query = NormalizedQueryFactory.createNormalizedQueryWithRawVariables(
            overallSchema,
            queryDocument,
            executionInput.operationName,
            executionInput.variables,
        )

        // TODO: determine what to do with NQ
        // instrumentation.beginExecute(NadelInstrumentationExecuteOperationParameters(query))

        val operationKind = getOperationKind(queryDocument, executionInput.operationName)

        val results = query.topLevelFields.map { topLevelField ->
            coroutineScope {
                async {
                    executeTopLevelField(topLevelField, operationKind, executionInput)
                }
            }
        }.awaitAll()

        return mergeTrees(results)
    }

    private suspend fun executeTopLevelField(
        topLevelField: NormalizedField,
        operationKind: OperationKind,
        executionInput: ExecutionInput,
    ): ExecutionResult {
        val service = getService(topLevelField, operationKind)

        val executionContext = NadelExecutionContext(executionInput)
        val executionPlan = executionPlanner.create(executionContext, services, service, topLevelField)
        val transformedQuery = queryTransformer.transformQuery(service, topLevelField, executionPlan).single()
        val result = executeService(service, transformedQuery, executionInput)
        val executionResult = resultTransformer.transform(executionContext, executionPlan, service, result)

        @Suppress("UNCHECKED_CAST")
        return ExecutionResultImpl.newExecutionResult()
            .data(executionResult.data)
            .errors(ErrorUtil.createGraphQlErrorsFromRawErrors(executionResult.errors))
            .extensions(executionResult.extensions as Map<Any, Any>)
            .build()
    }

    private fun getService(
        topLevelField: NormalizedField,
        operationKind: OperationKind,
    ): Service {
        // TODO: we need to support different services on the second level
        val topLevelFieldInfo = fieldInfos.getFieldInfo(operationKind, topLevelField.name)
            ?: error("Unknown top level field ${operationKind.displayName}.${topLevelField.name}")
        return topLevelFieldInfo.service
    }

    internal suspend fun executeHydration(
        service: Service,
        topLevelField: NormalizedField,
        pathToSourceField: List<String>,
        executionContext: NadelExecutionContext,
    ): ServiceExecutionResult {
        val sourceField = fold(initial = topLevelField, count = pathToSourceField.size - 1) {
            it.children.single()
        }

        // Creates N plans for the children then merges them together into one big plan
        val executionPlan = sourceField.children.map {
            executionPlanner.create(executionContext, services, service, rootField = it)
        }.reduce(NadelExecutionPlan::merge)

        // Transform the children of the source field
        // The source field itself is already transformed
        val sourceFieldWithTransformedChildren = sourceField.copyWithChildren(
            sourceField.children.flatMap {
                queryTransformer.transformQuery(service, field = it, executionPlan)
            },
        )

        // Get to the top level field again using .parent N times on the new source field
        val transformedQuery: NormalizedField = fold(
            initial = sourceFieldWithTransformedChildren,
            count = pathToSourceField.size - 1,
        ) {
            it.parent ?: error("No parent")
        }

        val result = executeService(service, transformedQuery, executionContext.executionInput)
        return resultTransformer.transform(executionContext, executionPlan, service, result)
    }

    private suspend fun executeService(
        service: Service,
        transformedQuery: NormalizedField,
        executionInput: ExecutionInput,
    ): ServiceExecutionResult {
        val document = compileToDocument(listOf(transformedQuery))

        return service.serviceExecution.execute(
            newServiceExecutionParameters()
                .query(document)
                .context(executionInput.context)
                .executionId(executionInput.executionId)
                .cacheControl(executionInput.cacheControl)
                .variables(emptyMap())
                .fragments(emptyMap())
                .operationDefinition(document.definitions.singleOfType())
                .serviceContext(null)
                .hydrationCall(false)
                .build()
        ).asDeferred().await()
    }

    private fun getOperationKind(queryDocument: Document, operationName: String?): OperationKind {
        val operation = NodeUtil.getOperation(queryDocument, operationName)
        return OperationKind.fromAst(operation.operationDefinition.operation)
    }

    private fun mergeTrees(results: List<ExecutionResult>): ExecutionResult {
        // TODO: merge these properly
        return results.first()
    }

    companion object {
        @JvmStatic
        fun newNadel(): Nadel.Builder {
            return Nadel.Builder().engineFactory(::NextgenEngine)
        }
    }
}

fun <T> fold(initial: T, count: Int, transform: (T) -> T): T {
    var element = initial
    for (i in 1..count) {
        element = transform(element)
    }
    return element
}

fun NormalizedField.toBuilder(): NormalizedField.Builder {
    var builder: NormalizedField.Builder? = null
    transform { builder = it }
    return builder!!
}

fun NormalizedField.copyWithChildren(children: List<NormalizedField>): NormalizedField {
    fun fixParents(old: NormalizedField?, new: NormalizedField?) {
        if (old == null || new == null || new.parent == null) {
            return
        }
        val newParent = new.parent.toBuilder()
            .children(old.parent.children.filter { it !== old } + new)
            .build()
        new.replaceParent(newParent)
        // Do recursively for all ancestors
        fixParents(old = old.parent, new = newParent)
    }

    return toBuilder()
        .children(children)
        .build()
        .also {
            fixParents(old = this, new = it)
        }
}
