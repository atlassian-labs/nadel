package graphql.nadel.engine

import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.OutstandingJobCounter.OutstandingJobHandle
import graphql.nadel.engine.util.copy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NadelDeferSupport internal constructor(
    private val delayedResultsChannel: Channel<DelayedIncrementalPartialResult> = Channel(UNLIMITED),
) {
    /**
     * The root [Job] to actually run the defer work.
     */
    private val deferCoroutineJob = SupervisorJob()
    private val deferCoroutineScope = CoroutineScope(deferCoroutineJob + Dispatchers.Default)

    /**
     * A single [Flow] that can only be collected from once.
     */
    private val resultFlow by lazy(delayedResultsChannel::consumeAsFlow)

    private val outstandingJobCounter = OutstandingJobCounter(
        onEmpty = {
            delayedResultsChannel.close()
        },
    )

    fun defer(task: suspend CoroutineScope.() -> DelayedIncrementalPartialResult): Job {
        val outstandingJobHandle = outstandingJobCounter.incrementJobCount()

        return deferCoroutineScope
            .launch {
                val hasNext: Boolean
                val result = try {
                    task()
                } finally {
                    hasNext = outstandingJobHandle.decrementAndGetJobCount() > 0
                }

                delayedResultsChannel.send(
                    // Copy of result but with the correct hasNext according to the info we know
                    result.copy(hasNext = hasNext)
                )
            }
    }

    /**
     * Assumes that the [Flow] will correctly set [DelayedIncrementalPartialResult.hasNext] correctly.
     *
     * i.e. that the last element in the [Flow] will have `hasNext` set to `false`.
     */
    fun defer(serviceResults: Flow<DelayedIncrementalPartialResult>): Job {
        val outstandingJobHandle = outstandingJobCounter.incrementJobCount()

        return deferCoroutineScope
            .launch {
                serviceResults
                    .collect { result ->
                        // Here we'll stipulate that the last element of the Flow sets hasNext=false
                        val hasNext = if (result.hasNext()) {
                            true
                        } else {
                            outstandingJobHandle.decrementAndGetJobCount() > 0
                        }


                        delayedResultsChannel.send(
                            // Copy of result but with the correct hasNext according to the info we know
                            result.copy(hasNext = hasNext)
                        )
                    }
            }
    }

    /**
     * Note that there is only one instance of this Flow, and it cannot be consumed more than once.
     *
     * There should never be more than one consumer. If you need multiple, you can wrap the [Flow] object.
     */
    fun resultFlow(): Flow<DelayedIncrementalPartialResult> {
        return resultFlow
    }

    fun close() {
        deferCoroutineScope.cancel()
    }
}

private class OutstandingJobCounter(
    private val onEmpty: () -> Unit,
) {
    private val count = AtomicInteger()

    fun incrementJobCount(): OutstandingJobHandle {
        count.incrementAndGet()

        val closed = AtomicBoolean(false)
        return OutstandingJobHandle {
            if (closed.getAndSet(true)) {
                throw IllegalArgumentException("Cannot close outstanding job more than once")
            }

            count.decrementAndGet()
                .also {
                    if (it == 0) {
                        onEmpty()
                    }
                }
        }
    }

    fun interface OutstandingJobHandle {
        fun decrementAndGetJobCount(): Int
    }
}
