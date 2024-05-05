package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.NadelExecutionHints
import graphql.nadel.NadelResultTracker
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.hooks.CreateServiceContextParams
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.normalized.ExecutableNormalizedOperation
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

data class NadelExecutionContext internal constructor(
    val executionInput: ExecutionInput,
    val query: ExecutableNormalizedOperation,
    internal val hooks: NadelExecutionHooks,
    val hints: NadelExecutionHints,
    val instrumentationState: InstrumentationState?,
    internal val timer: NadelInstrumentationTimer,
    internal val incrementalResultSupport: NadelIncrementalResultSupport,
    internal val resultTracker: NadelResultTracker,
    internal val hydrationDetails: ServiceExecutionHydrationDetails? = null,
) {
    private val serviceContexts = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    @Deprecated("Use incrementalSupport instead", ReplaceWith("incrementalResultSupport"))
    internal val deferSupport: NadelIncrementalResultSupport
        get() = incrementalResultSupport

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
            hooks.createServiceContext(
                CreateServiceContextParams(service)
            )
        }
    }
}
