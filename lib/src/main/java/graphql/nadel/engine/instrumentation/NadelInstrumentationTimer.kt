package graphql.nadel.enginekt.instrumentation

import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.Step
import java.time.Duration

internal class NadelInstrumentationTimer(
    private val instrumentation: NadelInstrumentation,
    private val userContext: Any?,
    private val instrumentationState: InstrumentationState?,
) {
    inline fun <T> time(
        step: Step,
        function: () -> T,
    ): T {
        val start = System.nanoTime()

        try {
            return function()
                .also {
                    val duration = getDurationSince(start)
                    instrumentation.onStepTimed(newParameters(step, duration))
                }
        } catch (e: Throwable) {
            try {
                val duration = getDurationSince(start)
                instrumentation.onStepTimed(newParameters(step, duration, exception = e))
            } catch (e2: Throwable) {
                e2.addSuppressed(e)
                throw e2
            }

            throw e
        }
    }

    fun batch(): BatchTimer {
        return BatchTimer(timer = this)
    }

    private fun newParameters(
        step: Step,
        duration: Duration,
        exception: Throwable? = null,
    ): NadelInstrumentationTimingParameters {
        return NadelInstrumentationTimingParameters(
            step,
            duration,
            exception = exception,
            userContext,
            instrumentationState
        )
    }

    class BatchTimer(
        private val timer: NadelInstrumentationTimer,
        private val timings: MutableMap<Step, Long> = mutableMapOf(),
    ) {
        inline fun <T> time(step: Step, function: () -> T): T {
            val start = System.nanoTime()
            return try {
                function()
            } finally {
                val end = System.nanoTime()
                timings[step] = (timings[step] ?: 0) + (end - start)
            }
        }

        fun submit() {
            timings.forEach { (step, durationNs) ->
                val params = timer.newParameters(step, Duration.ofNanos(durationNs))
                timer.instrumentation.onStepTimed(params)
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

    @Suppress("NOTHING_TO_INLINE") // inline anyway
    private inline fun getDurationSince(startNanos: Long): Duration {
        val end = System.nanoTime()
        return Duration.ofNanos(end - startNanos)
    }
}
