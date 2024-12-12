package graphql.nadel.time

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * Creates a stopped stopwatch. Thread safe.
 *
 * Can be stopped and started multiple times, will record the total time elapsed.
 */
class NadelStopwatch(
    private val ticker: () -> Long = defaultTicker,
) {
    /**
     * When the stopwatch was last started, and before that how much time was elapsed.
     */
    private data class Accumulator(
        val startedNs: Long? = null,
        val elapsedNs: Long,
    ) {
        companion object {
            val Zero = Accumulator(null, 0)
        }
    }

    private val accumulator = AtomicReference(Accumulator.Zero)

    fun start() {
        accumulator.getAndUpdate {
            if (it.startedNs == null) {
                it.copy(startedNs = ticker())
            } else {
                it
            }
        }
    }

    fun stop() {
        accumulator.getAndUpdate {
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
        val time = accumulator.get()
        val ns = if (time.startedNs == null) {
            time.elapsedNs
        } else {
            time.elapsedNs + (ticker() - time.startedNs)
        }
        return Duration.ofNanos(ns)
    }

    companion object {
        val defaultTicker = System::nanoTime
    }
}
