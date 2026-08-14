package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.NadelExecutionHints
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationCoalescingRound
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.result.NadelResultTracker
import graphql.normalized.ExecutableNormalizedOperation
import kotlinx.coroutines.CoroutineScope

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
    internal val isPartitionedCall: Boolean = false,
    internal val executionCoroutine: CoroutineScope,
) {
    internal var batchHydrationCoalescingParticipant: NadelBatchHydrationCoalescingRound.Participant? = null
        private set

    internal fun withBatchHydrationCoalescingParticipant(
        participant: NadelBatchHydrationCoalescingRound.Participant?,
    ): NadelExecutionContext {
        return copy().also { context ->
            context.batchHydrationCoalescingParticipant = participant
        }
    }

    val userContext: Any?
        get() {
            return executionInput.context
        }

    val graphQLContext: GraphQLContext
        get() {
            return executionInput.graphQLContext!!
    }
}
