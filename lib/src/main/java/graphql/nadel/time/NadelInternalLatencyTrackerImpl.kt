package graphql.nadel.time

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

open class NadelInternalLatencyTrackerImpl(
    /**
     * Stopwatch to track internal latency.
     */
    private val internalLatency: NadelStopwatch,
) : NadelInternalLatencyTracker {
    private val outstandingExternalLatencyCount = AtomicInteger()

    override fun getInternalLatency(): Duration {
        return internalLatency.elapsed()
    }

    fun onExternalRun(code: Runnable) {
        onExternalCallStart()

        try {
            code.run()
        } finally {
            onExternalCallEnd()
        }
    }

    fun <T : Any> onExternalGet(code: Supplier<T>): T {
        onExternalCallStart()

        try {
            return code.get()
        } finally {
            onExternalCallEnd()
        }
    }

    fun <T : Any> onExternalFuture(future: CompletableFuture<T>): CompletableFuture<T> {
        onExternalCallStart()

        return future
            .whenComplete { _, _ ->
                onExternalCallEnd()
            }
    }

    fun <T : Any> onExternalFuture(future: Supplier<CompletableFuture<T>>): CompletableFuture<T> {
        onExternalCallStart()

        return future.get()
            .whenComplete { _, _ ->
                onExternalCallEnd()
            }
    }

    protected fun onExternalCallStart() {
        if (outstandingExternalLatencyCount.getAndIncrement() == 0) {
            internalLatency.stop()
        }
    }

    protected fun onExternalCallEnd() {
        if (outstandingExternalLatencyCount.decrementAndGet() == 0) {
            internalLatency.start()
        }
    }
}
