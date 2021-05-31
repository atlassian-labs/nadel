package graphql.nadel

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.language.NodeUtil
import graphql.language.OperationDefinition
import graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprintFactory
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.plan.NadelExecutionPlanFactory
import graphql.nadel.enginekt.schema.NadelFieldInfos
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultTransformer
import graphql.nadel.enginekt.util.singleOfType
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
    private val fieldInfos = NadelFieldInfos.create(nadel.services)
    private val executionBlueprint = NadelExecutionBlueprintFactory.create(overallSchema, nadel.services)
    private val executionPlanner = NadelExecutionPlanFactory.create(executionBlueprint, nadel.overallSchema)
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
        // todo: we need to support different services on the second level
        val topLevelFieldInfo = fieldInfos.getFieldInfo(operationKind, topLevelField.name)
            ?: throw UnsupportedOperationException("Unknown top level field ${operationKind.displayName}.${topLevelField.name}")
        val service = topLevelFieldInfo.service

        val executionPlan = executionPlanner.create(executionInput.context, service, topLevelField)
        val result = executeService(service, executionPlan, topLevelField, executionInput)
        val executionResult = resultTransformer.transform(executionInput.context, executionPlan, service, result)

        @Suppress("UNCHECKED_CAST")
        return ExecutionResultImpl.newExecutionResult()
            .data(executionResult.data)
            .errors(ErrorUtil.createGraphQlErrorsFromRawErrors(executionResult.errors))
            .extensions(executionResult.extensions as Map<Any, Any>)
            .build()
    }

    private suspend fun executeService(
        service: Service,
        executionPlan: NadelExecutionPlan,
        topLevelField: NormalizedField,
        executionInput: ExecutionInput,
    ): ServiceExecutionResult {
        val transformedQuery = queryTransformer.transformQuery(service, topLevelField, executionPlan).single()
        val document = compileToDocument(listOf(transformedQuery))

        val serviceResult = service.serviceExecution.execute(
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

        return serviceResult
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
