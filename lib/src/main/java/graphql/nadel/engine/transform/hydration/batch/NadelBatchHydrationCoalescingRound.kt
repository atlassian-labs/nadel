package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.Service
import graphql.nadel.engine.transform.hydration.batch.NadelNewBatchHydrator.PreparedBatchHydration
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import kotlinx.coroutines.CompletableDeferred

/**
 * One backing error that must be located relative to a concrete source object after all result
 * mutations have produced the client-shaped response.
 */
internal data class NadelCoalescedBatchHydrationError(
    val rawError: JsonMap,
    val subject: JsonNode,
    val relativePath: List<Any>,
)

internal data class NadelCoalescedResultFieldOrder(
    val parent: JsonNode,
    val resultKeys: List<String>,
)

/**
 * The portion of a coalesced hydration result owned by one source execution.
 *
 * Ordinary data and request-level errors remain [NadelResultInstruction]s. Only errors that
 * need source-occurrence attribution use [locatedErrors], keeping the public instruction model
 * and the ordinary result-transform path unchanged.
 */
internal data class NadelCoalescedBatchHydrationOutput(
    val instructions: List<NadelResultInstruction> = emptyList(),
    val locatedErrors: List<NadelCoalescedBatchHydrationError> = emptyList(),
    val resultFieldOrders: List<NadelCoalescedResultFieldOrder> = emptyList(),
) {
    operator fun plus(other: NadelCoalescedBatchHydrationOutput): NadelCoalescedBatchHydrationOutput {
        if (this === EMPTY) return other
        if (other === EMPTY) return this
        return NadelCoalescedBatchHydrationOutput(
            instructions = instructions + other.instructions,
            locatedErrors = locatedErrors + other.locatedErrors,
            resultFieldOrders = resultFieldOrders + other.resultFieldOrders,
        )
    }

    companion object {
        val EMPTY = NadelCoalescedBatchHydrationOutput()
    }
}

/**
 * A request-scoped barrier for independently executing top-level source calls.
 *
 * Each participant may submit prepared batch hydrations and then completes. The last participant
 * dispatches the complete set, after which each participant receives only the output belonging
 * to its own source result. Calls are idempotent and a dispatcher failure is delivered to every
 * waiter.
 */
internal class NadelBatchHydrationCoalescingRound(
    participantCount: Int,
    private val enabledBackingServices: Set<Service>,
    private val dispatch: suspend (
        List<PreparedBatchHydration>,
    ) -> Map<PreparedBatchHydration, NadelCoalescedBatchHydrationOutput>,
) {
    private val lock = Any()
    private val finished = BooleanArray(participantCount)
    private val completions = List(participantCount) {
        CompletableDeferred<NadelCoalescedBatchHydrationOutput>()
    }
    private val submissions = mutableListOf<Submission>()
    private var nextSubmissionOrdinal = 0
    private var dispatchStarted = false

    init {
        require(participantCount > 0) {
            "A coalescing round must have at least one participant"
        }
    }

    fun participant(ordinal: Int): Participant {
        require(ordinal in completions.indices) {
            "Participant ordinal must be within the coalescing round"
        }
        return Participant(this, ordinal)
    }

    private fun trySubmit(
        participantOrdinal: Int,
        preparedHydration: PreparedBatchHydration,
    ): Boolean = synchronized(lock) {
        if (dispatchStarted || finished[participantOrdinal]) {
            false
        } else {
            submissions += Submission(
                participantOrdinal = participantOrdinal,
                submissionOrdinal = nextSubmissionOrdinal++,
                preparedHydration = preparedHydration,
            )
            true
        }
    }

    private suspend fun complete(
        participantOrdinal: Int,
    ): NadelCoalescedBatchHydrationOutput {
        val submissionsToDispatch = try {
            synchronized(lock) {
                if (!finished[participantOrdinal]) {
                    finished[participantOrdinal] = true
                }

                if (!dispatchStarted && finished.all { it }) {
                    dispatchStarted = true
                    submissions.toList()
                } else {
                    null
                }
            }
        } catch (throwable: Throwable) {
            abort(throwable)
            null
        }

        if (submissionsToDispatch != null) {
            try {
                val orderedSubmissions = submissionsToDispatch.sortedWith { left, right ->
                    left.participantOrdinal.compareTo(right.participantOrdinal)
                        .takeUnless { it == 0 }
                        ?: compareOperationFieldOrder(
                            left.preparedHydration.operationFieldOrder,
                            right.preparedHydration.operationFieldOrder,
                        ).takeUnless { it == 0 }
                        ?: left.submissionOrdinal.compareTo(right.submissionOrdinal)
                }
                val outputsByHydration = if (orderedSubmissions.isEmpty()) {
                    emptyMap()
                } else {
                    dispatch(orderedSubmissions.map(Submission::preparedHydration))
                }
                val outputsByParticipant = orderedSubmissions
                    .groupBy(Submission::participantOrdinal)
                    .mapValues { (_, participantSubmissions) ->
                        participantSubmissions.fold(NadelCoalescedBatchHydrationOutput.EMPTY) { output, submission ->
                            output + outputsByHydration
                                .getOrDefault(
                                    submission.preparedHydration,
                                    NadelCoalescedBatchHydrationOutput.EMPTY,
                                )
                        }
                    }

                completions.forEachIndexed { ordinal, completion ->
                    completion.complete(
                        outputsByParticipant.getOrDefault(
                            ordinal,
                            NadelCoalescedBatchHydrationOutput.EMPTY,
                        ),
                    )
                }
            } catch (throwable: Throwable) {
                abort(throwable)
            }
        }

        return completions[participantOrdinal].await()
    }

    private fun abort(throwable: Throwable) {
        synchronized(lock) {
            dispatchStarted = true
        }
        completions.forEach { completion ->
            completion.completeExceptionally(throwable)
        }
    }

    internal class Participant internal constructor(
        round: NadelBatchHydrationCoalescingRound,
        val ordinal: Int,
    ) {
        private val deliveryLock = Any()
        private var round: NadelBatchHydrationCoalescingRound? = round
        private var completionStarted = false
        private var outputDelivered = false

        fun isEnabledFor(backingService: Service): Boolean {
            return synchronized(deliveryLock) {
                round?.let { backingService in it.enabledBackingServices } == true
            }
        }

        fun trySubmit(preparedHydration: PreparedBatchHydration): Boolean {
            return synchronized(deliveryLock) {
                !completionStarted && round?.trySubmit(ordinal, preparedHydration) == true
            }
        }

        suspend fun complete() {
            val activeRound = synchronized(deliveryLock) {
                if (completionStarted) {
                    null
                } else {
                    completionStarted = true
                    round
                }
            }
            if (activeRound != null) {
                try {
                    activeRound.complete(ordinal)
                } catch (throwable: Throwable) {
                    activeRound.abort(throwable)
                    throw throwable
                } finally {
                    detach(activeRound)
                }
            }
        }

        fun abort(throwable: Throwable) {
            val activeRound = synchronized(deliveryLock) {
                completionStarted = true
                round.also { round = null }
            }
            activeRound?.abort(throwable)
        }

        suspend fun completeAndTakeOutput(): NadelCoalescedBatchHydrationOutput {
            val activeRound = synchronized(deliveryLock) {
                completionStarted = true
                round
            } ?: return NadelCoalescedBatchHydrationOutput.EMPTY
            val output = try {
                activeRound.complete(ordinal)
            } catch (throwable: Throwable) {
                activeRound.abort(throwable)
                throw throwable
            } finally {
                detach(activeRound)
            }
            return synchronized(deliveryLock) {
                if (outputDelivered) {
                    NadelCoalescedBatchHydrationOutput.EMPTY
                } else {
                    outputDelivered = true
                    output
                }
            }
        }

        private fun detach(activeRound: NadelBatchHydrationCoalescingRound) {
            synchronized(deliveryLock) {
                if (round === activeRound) {
                    round = null
                }
            }
        }
    }

    private data class Submission(
        val participantOrdinal: Int,
        val submissionOrdinal: Int,
        val preparedHydration: PreparedBatchHydration,
    )

    private companion object {
        fun compareOperationFieldOrder(left: List<Int>, right: List<Int>): Int {
            for (index in 0 until minOf(left.size, right.size)) {
                val comparison = left[index].compareTo(right[index])
                if (comparison != 0) {
                    return comparison
                }
            }
            return left.size.compareTo(right.size)
        }
    }
}
