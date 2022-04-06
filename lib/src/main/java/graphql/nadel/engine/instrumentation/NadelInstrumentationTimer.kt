package graphql.nadel.engine.instrumentation

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

        val result = try {
            function()
        } catch (e: Throwable) {
            try {
                emit(step, startNs = start, exception = e)
            } catch (e2: Throwable) {
                e2.addSuppressed(e)
                throw e2
            }

            throw e
        }

        emit(step, startNs = start)

        return result
    }

    fun batch(): BatchTimer {
        return BatchTimer(timer = this)
    }

    inline fun <T> batch(function: (BatchTimer) -> T): T {
        return BatchTimer(timer = this).use(function)
    }

    @Suppress("NOTHING_TO_INLINE") // inline anyway
    private inline fun emit(step: Step, startNs: Long, exception: Throwable? = null) {
        val endNs = System.nanoTime()
        val duration = Duration.ofNanos(endNs - startNs)
        emit(step, duration, exception)
    }

    @Suppress("NOTHING_TO_INLINE") // inline anyway
    private inline fun emit(step: Step, duration: Duration, exception: Throwable? = null) {
        instrumentation.onStepTimed(newParameters(step, duration, exception))
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
