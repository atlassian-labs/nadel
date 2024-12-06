package graphql.nadel.time

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
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
            internalLatency.stop()
        }
    }

    protected fun onExternalCallEnd() {
        if (outstandingExternalLatencyCount.decrementAndGet() == 0) {
            internalLatency.start()
        }
    }
}
