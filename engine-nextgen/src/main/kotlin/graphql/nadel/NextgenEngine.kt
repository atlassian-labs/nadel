package graphql.nadel

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.language.NodeUtil
import graphql.language.OperationDefinition
import graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters
import graphql.nadel.enginekt.normalized.NormalizedQueryToDocument
import graphql.nadel.enginekt.plan.GraphQLQueryPlanner
import graphql.nadel.enginekt.plan.GraphQLQueryPlan
import graphql.nadel.enginekt.schema.GraphQLFieldInfos
import graphql.nadel.enginekt.transform.query.GraphQLQueryTransformer
import graphql.nadel.enginekt.util.singleOfType
import graphql.nadel.util.ErrorUtil
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQueryTreeFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import java.util.concurrent.CompletableFuture

class NextgenEngine(nadel: Nadel) : NadelExecutionEngine {
    private val schema = nadel.overallSchema
    private val fieldInfos = GraphQLFieldInfos(nadel.overallSchema, nadel.services)
    private val queryPlanner = GraphQLQueryPlanner.create(nadel.overallSchema)
    private val queryTransformer = GraphQLQueryTransformer.create(nadel.overallSchema)
    private val instrumentation = nadel.instrumentation
    private val normalizedQueryToDocument = NormalizedQueryToDocument()

    override fun execute(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?,
        nadelExecutionParams: NadelExecutionParams
    ): CompletableFuture<ExecutionResult> {
        return GlobalScope.async {
            executeCoroutine(executionInput, queryDocument, instrumentationState)
        }.asCompletableFuture()
    }

    private suspend fun executeCoroutine(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?
    ): ExecutionResult {

        val query = NormalizedQueryTreeFactory.createNormalizedQuery(
            schema,
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
                    execute(topLevelField, operationKind, executionInput)
                }
            }
        }.awaitAll()

        return mergeTrees(results)
    }

    private suspend fun execute(
        topLevelField: NormalizedField,
        operationKind: OperationKind,
        executionInput: ExecutionInput,
    ): ExecutionResult {
        val topLevelFieldInfo = fieldInfos.getFieldInfo(operationKind, topLevelField.name)
            ?: throw UnsupportedOperationException("Unknown top level field ${operationKind.displayName}.${topLevelField.name}")
        val service = topLevelFieldInfo.service

        val queryPlan = queryPlanner.generate(executionInput.context, service, topLevelField)

        val executionResult = postProcess(
            queryPlan,
            executeService(service, topLevelField, executionInput),
        )

        @Suppress("UNCHECKED_CAST")
        return ExecutionResultImpl.newExecutionResult()
            .data(executionResult.data)
            .errors(ErrorUtil.createGraphQlErrorsFromRawErrors(executionResult.errors))
            .extensions(executionResult.extensions as Map<Any, Any>)
            .build()
    }

    private suspend fun executeService(
        service: Service,
        topLevelField: NormalizedField,
        executionInput: ExecutionInput,
    ): ServiceExecutionResult {
        val transformedQuery = queryTransformer.transform(executionInput.context, topLevelField).single()
        val document = normalizedQueryToDocument.toDocument(transformedQuery)

        return service.serviceExecution.execute(
            newServiceExecutionParameters()
                .query(document)
                .context(executionInput.context)
                .executionId(executionInput.executionId)
                .cacheControl(executionInput.cacheControl)
                .variables(emptyMap())
                .fragments(emptyMap())
                .operationDefinition(document.definitions.singleOfType<OperationDefinition>())
                .serviceContext(null)
                .hydrationCall(false)
                .build()
        ).asDeferred().await()
    }

    private fun postProcess(queryPlan: GraphQLQueryPlan, result: ServiceExecutionResult): ServiceExecutionResult {
        // TODO: run through schema and result transformer here
        return result
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
