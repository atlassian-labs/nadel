package graphql.nadel.engine.instrumentation

import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.Step
import java.time.Duration
import java.time.Instant

internal class NadelInstrumentationTimer(
    private val instrumentation: NadelInstrumentation,
    private val userContext: Any?,
    private val instrumentationState: InstrumentationState?,
) {
    inline fun <T> time(
        step: Step,
        function: () -> T,
    ): T {
        val start = Instant.now()
        val startNs = System.nanoTime()

        val result = try {
            function()
        } catch (e: Throwable) {
            try {
                emit(step, start = start, startNs = startNs, exception = e)
            } catch (e2: Throwable) {
                e2.addSuppressed(e)
                throw e2
            }

            throw e
        }

        emit(step, start, startNs = startNs)

        return result
    }

    fun batch(): BatchTimer {
        return BatchTimer(timer = this)
    }

    inline fun <T> batch(function: (BatchTimer) -> T): T {
        return BatchTimer(timer = this).use(function)
    }

    @Suppress("NOTHING_TO_INLINE") // inline anyway
    private inline fun emit(step: Step, start: Instant, startNs: Long, exception: Throwable? = null) {
        val endNs = System.nanoTime()
        val duration = Duration.ofNanos(endNs - startNs)

        instrumentation.onStepTimed(newParameters(step, start, duration, exception))
    }

    @Suppress("NOTHING_TO_INLINE") // inline anyway
    private inline fun emit(step: Step, duration: Duration, exception: Throwable? = null) {
        instrumentation.onStepTimed(newParameters(step, null, duration, exception))
    }

    private fun newParameters(
        step: Step,
        startedAt: Instant?,
        duration: Duration,
        exception: Throwable? = null,
    ): NadelInstrumentationTimingParameters {
        return NadelInstrumentationTimingParameters(
            step = step,
            startedAt = startedAt,
            duration = duration,
            exception = exception,
            context = userContext,
            instrumentationState = instrumentationState,
        )
    }

    class BatchTimer(
        private val timer: NadelInstrumentationTimer,
        private val timings: MutableMap<Step, Long> = mutableMapOf(),
        private val scraps: MutableMap<Step, Long> = mutableMapOf(),
    ) {
        private var exception: Throwable? = null

        inline fun <T> time(step: Step, function: () -> T): T {
            val start = System.nanoTime()
            return try {
                function()
            } catch (e: Throwable) {
                exception = e
                throw e
            } finally {
                val end = System.nanoTime()
                val duration = end - start
                if (duration < 1000000) {
                    scraps[step] = (timings[step] ?: 0) + (end - start)
                }

                timings[step] = (timings[step] ?: 0) + (end - start)
            }
        }

        fun submit() {
            timings.forEach { (step, durationNs) ->
                timer.emit(step, duration = Duration.ofNanos(durationNs), exception)
            }
        }

        inline fun <T> use(function: (timer: BatchTimer) -> T): T {
            try {
                return function(this)
            } finally {
                submit()
            }
        }

        override fun toString(): String {
            return "BatchTimer(timings=$timings)"
        }
    }
}
