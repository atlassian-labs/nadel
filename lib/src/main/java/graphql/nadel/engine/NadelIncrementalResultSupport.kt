package graphql.nadel.engine

import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.engine.NadelIncrementalResultSupport.OutstandingJobCounter.OutstandingJobHandle
import graphql.nadel.util.getLogger
import graphql.normalized.ExecutableNormalizedOperation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NadelIncrementalResultSupport internal constructor(
    lazyAccumulator: Lazy<NadelIncrementalResultAccumulator>,
    private val delayedResultsChannel: Channel<DelayedIncrementalPartialResult> = makeDefaultChannel(),
) {
    private val accumulator by lazyAccumulator

    internal constructor(
        operation: ExecutableNormalizedOperation,
        delayedResultsChannel: Channel<DelayedIncrementalPartialResult> = makeDefaultChannel(),
    ) : this(
        lazyAccumulator = lazy {
            NadelIncrementalResultAccumulator(
                operation = operation,
            )
        },
        delayedResultsChannel = delayedResultsChannel,
    )

    companion object {
        private val log = getLogger<NadelIncrementalResultSupport>()

        private fun makeDefaultChannel(): Channel<DelayedIncrementalPartialResult> = Channel(
            capacity = 100,
            onBufferOverflow = BufferOverflow.DROP_LATEST,
            onUndeliveredElement = {
                log.error("Dropping incremental result because of buffer overflow")
            },
        )
    }

    private val operationMutex = Mutex()

    /**
     * The root [Job] to run the defer and stream work etc on.
     */
    private val coroutineJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineJob + Dispatchers.Default)

    /**
     * Temporary _kind of_ hack to wait for the initial result to complete before kicking off other jobs.
     *
     * Doesn't really handle a defer job kicking off more deferrals, but we'll cross that bridge later.
     */
    private val initialCompletionLock = CompletableDeferred<Unit>()

    /**
     * A single [Flow] that can only be collected from once.
     */
    private val resultFlow by lazy(delayedResultsChannel::consumeAsFlow)

    init {
        coroutineJob.invokeOnCompletion {
            require(outstandingJobCounter.isEmpty())
            delayedResultsChannel.close()
        }
    }

    private val outstandingJobCounter = OutstandingJobCounter()

    fun defer(task: suspend CoroutineScope.() -> DelayedIncrementalPartialResult): Job {
        return launch { outstandingJobHandle ->
            val result = task()
            initialCompletionLock.await()

            operationMutex.withLock {
                accumulator.accumulate(result)

                val hasNext = outstandingJobHandle.decrementAndGetJobCount() > 0

                val next = accumulator.getIncrementalPartialResult(hasNext)
                if (next != null) {
                    delayedResultsChannel.send(next)
                } else if (!hasNext) {
                    delayedResultsChannel.send(emptyLastResult())
                }
            }
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
                    initialCompletionLock.await()

                    operationMutex.withLock {
                        accumulator.accumulate(result)

                        // Here we'll stipulate that the last element of the Flow sets hasNext=false
                        val hasNext = if (result.hasNext()) {
                            true
                        } else {
                            outstandingJobHandle.decrementAndGetJobCount() > 0
                        }

                        val next = accumulator.getIncrementalPartialResult(hasNext)
                        if (next != null) {
                            delayedResultsChannel.send(next)
                        } else if (!hasNext) {
                            delayedResultsChannel.send(emptyLastResult())
                        }
                    }
                }
        }
    }

    fun hasDeferredResults(): Boolean {
        return outstandingJobCounter.hadJobs
    }

    /**
     * Note that there is only one instance of this Flow, and it cannot be consumed more than once.
     *
     * There should never be more than one consumer. If you need multiple, you can wrap the [Flow] object.
     */
    fun resultFlow(): Flow<DelayedIncrementalPartialResult> {
        return resultFlow.onCompletion {
            close()
        }
    }

    fun onInitialResultComplete() {
        // This signals the end for the job; not immediately, but as soon as the child jobs are all done
        coroutineJob.complete()

        // Unblocks work to yield results to the channel
        initialCompletionLock.complete(Unit)
    }

    private fun close() {
        coroutineScope.cancel()
    }

    /**
     * We have to return hasNext=false to indicate to clients that there's no more data coming.
     * Note: the spec allows an empty payload which only contains hastNext=false to be returned.
     */
    private fun emptyLastResult(): DelayedIncrementalPartialResult {
        return DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
            .incrementalItems(emptyList())
            .hasNext(false)
            .build()
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
            coroutineScope
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
        private val _hadJobs = AtomicBoolean()
        private val count = AtomicInteger()

        val hadJobs: Boolean
            get() = _hadJobs.get()

        fun isEmpty(): Boolean {
            return count.get() == 0
        }

        fun incrementJobCount(): OutstandingJobHandle {
            count.incrementAndGet()
            _hadJobs.set(true)

            val closed = AtomicBoolean(false)
            return object : OutstandingJobHandle {
                override fun decrementAndGetJobCount(): Int {
                    return if (closed.getAndSet(true)) {
                        throw IllegalStateException("Cannot close outstanding job more than once")
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
