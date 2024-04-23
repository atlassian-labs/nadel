package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.hooks.CreateServiceContextParams
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.normalized.ExecutableNormalizedOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

data class NadelExecutionContext internal constructor(
    /**
     * A scope close to the root of the request execution, use this to launch
     * async jobs still tied to the request.
     */
    internal val coroutineScope: CoroutineScope,
    internal val resultsChannel: Channel<DelayedIncrementalPartialResult>,
    val executionInput: ExecutionInput,
    val query: ExecutableNormalizedOperation,
    internal val hooks: NadelExecutionHooks,
    val hints: NadelExecutionHints,
    val instrumentationState: InstrumentationState?,
    internal val timer: NadelInstrumentationTimer,
) {
    private val serviceContexts = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    internal val deferScope = CoroutineScope(SupervisorJob()) // get() = coroutineScope

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
