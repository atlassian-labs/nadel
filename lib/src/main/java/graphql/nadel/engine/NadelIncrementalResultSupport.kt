package graphql.nadel.engine

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.NadelIncrementalResultSupport.OutstandingJobCounter.OutstandingJobHandle
import graphql.nadel.engine.util.copy
import graphql.nadel.util.getLogger
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import graphql.normalized.incremental.NormalizedDeferredExecution
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class NadelIncrementalResultSupport internal constructor(
    private val delayedResultsChannel: Channel<DelayedIncrementalPartialResult> = Channel(
        capacity = 100,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
        onUndeliveredElement = {
            log.error("Dropping incremental result because of buffer overflow")
        },
    ),

    private val operation: ExecutableNormalizedOperation? = null,
) {
    companion object {
        private val log = getLogger<NadelIncrementalResultSupport>()
    }

    private val channelMutex = Mutex()

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

    private val deferGroups: DeferGroups?

    init {
        coroutineJob.invokeOnCompletion {
            require(outstandingJobCounter.isEmpty())
            delayedResultsChannel.close()
        }

        if (operation != null) {
            deferGroups = parseDeferGroups(operation)
        } else {
            deferGroups = null
        }
    }

    private fun parseDeferGroups(operation: ExecutableNormalizedOperation): DeferGroups {
        val fieldsByDefer = operation
            .walkTopDown()
            .filter {
                it.deferredExecutions.isNotEmpty()
            }
            .groupBy { field ->
                // todo: what do when there's multiple?
                field.deferredExecutions.single()
            }

        val deferByFields = operation
            .walkTopDown()
            .filter {
                it.deferredExecutions.isNotEmpty()
            }
            .associateWith {
                // todo: what do when there's multiple?
                it.deferredExecutions.single()
            }

        return DeferGroups(
            fieldsByDefer,
            deferByFields,
        )
    }

    /**
     * We are currently making the assumption that 1 field cannot span multiple defer groups
     */
    data class DeferGroups(
        val fieldsByDefer: Map<NormalizedDeferredExecution, List<ExecutableNormalizedField>>,
        val deferByFields: Map<ExecutableNormalizedField, NormalizedDeferredExecution>,
    )

    data class DeferGroupKey(
        val path: List<Any>,
        val execution: NormalizedDeferredExecution,
    )

    private val accumulatingDeferGroups = mutableMapOf<DeferGroupKey, MutableMap<String, Any?>>()

    private val outstandingJobCounter = OutstandingJobCounter()

    fun defer(task: suspend CoroutineScope.() -> DelayedIncrementalPartialResult): Job {
        return launch { outstandingJobHandle ->
            val result = task()
            initialCompletionLock.await()

            process(result)

            channelMutex.withLock {
                val hasNext = outstandingJobHandle.decrementAndGetJobCount() > 0

                delayedResultsChannel.send(
                    // Copy of result but with the correct hasNext according to the info we know
                    quickCopy(result, hasNext),
                )
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

                    process(result)

                    channelMutex.withLock {
                        // Here we'll stipulate that the last element of the Flow sets hasNext=false
                        val hasNext = if (result.hasNext()) {
                            true
                        } else {
                            outstandingJobHandle.decrementAndGetJobCount() > 0
                        }

                        delayedResultsChannel.send(
                            // Copy of result but with the correct hasNext according to the info we know
                            quickCopy(result, hasNext),
                        )
                    }
                }
        }
    }

    private fun process(result: DelayedIncrementalPartialResult) {
        result.incremental
            ?.forEach { payload ->
                when (payload) {
                    is DeferPayload -> {
                        val data = payload.getData<Map<String, Any?>?>()!! // todo: what happens if data is null?

                        data.forEach { (key, value) ->
                            val deferExecution = deferGroups!!.deferByFields
                                .entries
                                .find { (field) ->
                                    if (payload.path.isEmpty()) {
                                        field.parent == null && field.resultKey == key
                                    } else {
                                        field.parent.listOfResultKeys == payload.path.filterIsInstance<String>()
                                            && field.resultKey == key
                                    }
                                }!!
                                .value

                            val deferKey = DeferGroupKey(payload.path, deferExecution)

                            val accumulator = accumulatingDeferGroups.computeIfAbsent(deferKey) {
                                mutableMapOf()
                            }

                            accumulator.put(key, value)
                            if (accumulator.size == deferGroups!!.fieldsByDefer[deferExecution]!!.size) {
                                println("hello\n$accumulator")
                            }
                        }
                    }
                    else -> {}
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
        return resultFlow
    }

    fun onInitialResultComplete() {
        // This signals the end for the job; not immediately, but as soon as the child jobs are all done
        coroutineJob.complete()

        // Unblocks work to yield results to the channel
        initialCompletionLock.complete(Unit)
    }

    fun close() {
        coroutineScope.cancel()
    }

    private fun quickCopy(
        subject: DelayedIncrementalPartialResult,
        hasNext: Boolean,
    ): DelayedIncrementalPartialResult {
        return if (subject.hasNext() == hasNext) {
            subject
        } else {
            subject.copy(hasNext = hasNext)
        }
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

/**
 * Similar to [java.io.File.walkTopDown] but for ENFs.
 */
fun ExecutableNormalizedOperation.walkTopDown(): Sequence<ExecutableNormalizedField> {
    return topLevelFields
        .asSequence()
        .flatMap {
            it.walkTopDown()
        }
}

/**
 * Similar to [java.io.File.walkTopDown] but for ENFs.
 */
fun ExecutableNormalizedField.walkTopDown(): Sequence<ExecutableNormalizedField> {
    return sequenceOf(this) + children.asSequence()
        .flatMap {
            it.walkTopDown()
        }
}

