package graphql.nadel.time

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

interface NadelInternalLatencyTracker {
    /**
     * Starts tracking internal latency.
     *
     * Should be invoked for any of the following
     * * As soon as the GraphQL request is received.
     * * As soon as all external calls complete.
     */
    fun start()

    /**
     * Stops tracking internal latency.
     *
     * Should be invoked for any of the following
     * * When the GraphQL request is sent back to the caller.
     * * When any external call starts.
     */
    fun stop()

    /**
     * Gets the _current_ internal latency.
     *
     * This can be invoked before the latency is completely tracked to get a running track
     * of latency.
     */
    fun getInternalLatency(): Duration

    open class Default(
        /**
         * Stopwatch to track internal latency.
         */
        private val internalLatency: NadelStopwatch,
    ) : NadelInternalLatencyTracker {
        private val outstandingExternalLatencyCount = AtomicInteger()

        override fun start() {
            internalLatency.start()
        }

        override fun stop() {
            internalLatency.stop()
        }

        override fun getInternalLatency(): Duration {
            return internalLatency.elapsed()
        }

        fun onExternalCall(code: Runnable) {
            onExternalCallStart()

            try {
                code.run()
            } finally {
                onExternalCallEnd()
            }
        }

        fun <T : Any> onExternalCall(code: Supplier<T>): T {
            onExternalCallStart()

            try {
                return code.get()
            } finally {
                onExternalCallEnd()
            }
        }

        fun <T : Any> onExternalCall(future: CompletableFuture<T>): CompletableFuture<T> {
            onExternalCallStart()

            return future
                .whenComplete { _, _ ->
                    onExternalCallEnd()
                }
        }

        fun <T : Any> onExternalCall(future: Supplier<CompletableFuture<T>>): CompletableFuture<T> {
            onExternalCallStart()

            return future.get()
                .whenComplete { _, _ ->
                    onExternalCallEnd()
                }
        }

        fun <T, P : Publisher<T>> onExternalCall(publisher: Publisher<T>): Publisher<T> {
            return Publisher<T> { realSubscriber ->
                publisher.subscribe(
                    object : Subscriber<T> {
                        override fun onSubscribe(s: Subscription) {
                            onExternalCallStart()
                            realSubscriber.onSubscribe(s)
                        }

                        override fun onError(t: Throwable?) {
                            onExternalCallEnd()
                            realSubscriber.onError(t)
                        }

                        override fun onComplete() {
                            onExternalCallEnd()
                            realSubscriber.onComplete()
                        }

                        override fun onNext(t: T) {
                            realSubscriber.onNext(t)
                        }
                    }
                )
            }
        }

        protected fun onExternalCallStart() {
            if (outstandingExternalLatencyCount.getAndIncrement() == 0) {
                stop()
            }
        }

        protected fun onExternalCallEnd() {
            if (outstandingExternalLatencyCount.decrementAndGet() == 0) {
                start()
            }
        }
    }
}

