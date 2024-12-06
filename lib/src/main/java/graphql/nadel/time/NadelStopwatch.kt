package graphql.nadel.time

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * Creates a stopped stopwatch. Thread safe.
 */
class NadelStopwatch(
    private val ticker: () -> Long = defaultTicker,
) {
    private data class Time(
        val startedNs: Long? = null,
        val elapsedNs: Long,
    ) {
        companion object {
            val Zero = Time(null, 0)
        }
    }

    private val time = AtomicReference(Time.Zero)

    fun start() {
        time.getAndUpdate {
            if (it.startedNs == null) {
                it.copy(startedNs = ticker())
            } else {
                it
            }
        }
    }

    fun stop() {
        time.getAndUpdate {
            if (it.startedNs == null) {
                it
            } else {
                it.copy(
                    startedNs = null,
                    elapsedNs = it.elapsedNs + (ticker() - it.startedNs),
                )
            }
        }
    }

    fun elapsed(): Duration {
        val time = time.get()
        val ns = if (time.startedNs == null) {
            time.elapsedNs
        } else {
            ticker() - time.startedNs + time.elapsedNs
        }
        return Duration.ofNanos(ns)
    }

    companion object {
        val defaultTicker = System::nanoTime
    }
}
