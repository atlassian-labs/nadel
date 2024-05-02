package graphql.nadel.engine

import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.NadelDeferSupport.OutstandingJobCounter.OutstandingJobHandle
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

    init {
        deferCoroutineJob.invokeOnCompletion {
            require(outstandingJobCounter.isEmpty())
            delayedResultsChannel.close()
        }
    }

    private val outstandingJobCounter = OutstandingJobCounter()

    fun defer(task: suspend CoroutineScope.() -> DelayedIncrementalPartialResult): Job {
        return launch { outstandingJobHandle ->
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
        return launch { outstandingJobHandle ->
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

    fun onInitialResultComplete() {
        deferCoroutineJob.complete()
    }

    fun close() {
        deferCoroutineScope.cancel()
    }

    /**
     * Launches a job and increments the outstanding job handle.
     *
     * Ensures the outstanding job is always closed at the end.
     */
    private inline fun launch(
        crossinline task: suspend CoroutineScope.(OutstandingJobHandle) -> Unit,
    ): Job {
        val outstandingJobHandle = outstandingJobCounter.incrementJobCount()

        return try {
            deferCoroutineScope
                .launch {
                    task(outstandingJobHandle)
                }
                .also { job ->
                    job.invokeOnCompletion {
                        outstandingJobHandle.onFinallyEnsureDecremented()
                    }
                }
        } catch (e: Throwable) {
            outstandingJobHandle.onFinallyEnsureDecremented()
            throw e
        }
    }

    private class OutstandingJobCounter {
        private val count = AtomicInteger()

        fun isEmpty(): Boolean {
            return count.get() == 0
        }

        fun incrementJobCount(): OutstandingJobHandle {
            count.incrementAndGet()

            val closed = AtomicBoolean(false)
            return object : OutstandingJobHandle {
                override fun decrementAndGetJobCount(): Int {
                    return if (closed.getAndSet(true)) {
                        throw IllegalArgumentException("Cannot close outstanding job more than once")
                    } else {
                        count.decrementAndGet()
                    }
                }

                override fun onFinallyEnsureDecremented() {
                    if (!closed.getAndSet(true)) {
                        count.decrementAndGet()
                    }
                }
            }
        }

        interface OutstandingJobHandle {
            fun decrementAndGetJobCount(): Int
            fun onFinallyEnsureDecremented()
        }
    }
}
