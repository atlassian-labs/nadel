package graphql.nadel.time

import java.io.Closeable
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

open class NadelInternalLatencyTrackerImpl(
    /**
     * Stopwatch to track internal latency.
     */
    private val internalLatency: NadelStopwatch,
) : NadelInternalLatencyTracker {
    private val lock = Any()

    @Volatile
    private var outstandingExternalLatencyCount = 0

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

        try {
            return future.get()
                .whenComplete { _, _ ->
                    call.close()
                }
        } catch (e: Throwable) {
            call.close()
            throw e
        }
    }

    /**
     * Used to ensure that at the end of a request, there are no outstanding external calls.
     *
     * @return true if all external calls were closed
     */
    fun noOutstandingCalls(): Boolean {
        return outstandingExternalLatencyCount == 0
    }

    private inner class ExternalCall : Closeable {
        /**
         * Used to ensure the call does not decrement the counter more than once.
         */
        private val closed = AtomicBoolean(false)

        init {
            synchronized(lock) {
                if ((++outstandingExternalLatencyCount) == 1) {
                    internalLatency.stop()
                }
            }
        }

        override fun close() {
            if (!closed.getAndSet(true)) {
                synchronized(lock) {
                    if ((--outstandingExternalLatencyCount) == 0) {
                        internalLatency.start()
                    }
                }
            }
        }
    }
}
