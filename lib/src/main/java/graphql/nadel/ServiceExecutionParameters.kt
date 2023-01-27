package graphql.nadel

import graphql.GraphQLContext
import graphql.execution.ExecutionId
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.util.provide
import graphql.nadel.engine.util.singleOfType
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperationToAstCompiler
import kotlinx.coroutines.future.await

class ServiceExecutionParameters internal constructor(
    val query: Document,
    val context: Any?,
    val graphQLContext: GraphQLContext,
    val variables: Map<String, Any>,
    val operationDefinition: OperationDefinition,
    val executionId: ExecutionId,
    private val serviceContext: Any?,
    /**
     * @return details abut this service hydration or null if it's not a hydration call
     */
    val hydrationDetails: ServiceExecutionHydrationDetails?,
    val executableNormalizedField: ExecutableNormalizedField,
) {
    fun <T> getServiceContext(): T? {
        @Suppress("UNCHECKED_CAST") // Trust caller
        return serviceContext as T?
    }

    val isHydrationCall: Boolean
        get() = hydrationDetails != null

    internal object Factory {
        context(NadelEngineContext, NadelExecutionContext)
        suspend fun get(
            service: Service,
            compileResult: ExecutableNormalizedOperationToAstCompiler.CompilerResult,
            hydrationDetails: ServiceExecutionHydrationDetails?,
            query: ExecutableNormalizedField,
        ): ServiceExecutionParameters {
            return ServiceExecutionParameters(
                query = compileResult.document,
                context = executionInput.context,
                graphQLContext = executionInput.graphQLContext,
                executionId = executionInput.executionId ?: executionIdProvider.provide(executionInput),
                variables = compileResult.variables,
                operationDefinition = compileResult.document.definitions.singleOfType(),
                serviceContext = getContextForService(service).await(),
                hydrationDetails = hydrationDetails,
                executableNormalizedField = query,
            )
        }
    }
}

