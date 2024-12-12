package graphql.nadel.time

import java.io.Closeable
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
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

    fun newExternalCall(): Closeable {
        return ExternalCall()
    }

    fun onExternalRun(code: Runnable) {
        newExternalCall().use {
            code.run()
        }
    }

    fun <T : Any> onExternalGet(code: Supplier<T>): T {
        return newExternalCall().use {
            code.get()
        }
    }

    fun <T : Any> onExternalFuture(future: CompletableFuture<T>): CompletableFuture<T> {
        val call = newExternalCall()

        return future
            .whenComplete { _, _ ->
                call.close()
            }
    }

    fun <T : Any> onExternalFuture(future: Supplier<CompletableFuture<T>>): CompletableFuture<T> {
        val call = newExternalCall()

        return future.get()
            .whenComplete { _, _ ->
                call.close()
            }
    }

    /**
     * Used to ensure that at the end of a request, there are no outstanding external calls.
     *
     * @return true if all external calls were closed
     */
    fun noOutstandingCalls(): Boolean {
        return outstandingExternalLatencyCount.get() == 0
    }

    private inner class ExternalCall : Closeable {
        /**
         * Used to ensure the call does not decrement the counter more than once.
         */
        private val closed = AtomicBoolean(false)

        init {
            if (outstandingExternalLatencyCount.getAndIncrement() == 0) {
                internalLatency.stop()
            }
        }

        override fun close() {
            if (!closed.getAndSet(true)) {
                if (outstandingExternalLatencyCount.decrementAndGet() == 0) {
                    internalLatency.start()
                }
            }
        }
    }
}
