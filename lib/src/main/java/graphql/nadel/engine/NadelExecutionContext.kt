package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.hooks.CreateServiceContextParams
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.normalized.ExecutableNormalizedOperation
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

data class NadelExecutionContext internal constructor(
    val executionInput: ExecutionInput,
    val inputOperation: ExecutableNormalizedOperation,
    val inputQueryDocument: Document,
    internal val serviceExecutionHooks: ServiceExecutionHooks,
    val executionHints: NadelExecutionHints,
    val instrumentationState: InstrumentationState?,
    internal val nadelTimer: NadelInstrumentationTimer,
) {
    private val serviceContexts = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    val userContext: Any?
        get() {
            return executionInput.context
        }

    val graphQLContext: GraphQLContext
        get() {
            return executionInput.graphQLContext!!
        }

    /**
     * Get the service context for a given service
     */
    fun getContextForService(service: Service): CompletableFuture<Any?> {
        return serviceContexts.getOrPut(service.name) {
            serviceExecutionHooks.createServiceContext(
                CreateServiceContextParams(service)
            )
        }
    }
}
