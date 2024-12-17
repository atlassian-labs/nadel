package graphql.nadel.time

import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.Step
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

internal class NadelInstrumentationTimer(
    private val isEnabled: Boolean,
    private val ticker: () -> Duration,
    private val instrumentation: NadelInstrumentation,
    private val userContext: Any?,
    private val instrumentationState: InstrumentationState?,
) {
    inline fun <T> time(
        step: Step,
        function: () -> T,
    ): T {
        if (!isEnabled) {
            return function()
        }

        val start = ticker()

        val result = try {
            function()
        } catch (e: Throwable) {
            try {
                emit(
                    step = step,
                    internalLatency = ticker() - start,
                    exception = e,
                )
            } catch (e2: Throwable) {
                e2.addSuppressed(e)
                throw e2
            }

            throw e
        }

        emit(
            step = step,
            internalLatency = ticker() - start,
        )

        return result
    }

    fun batch(): BatchTimer {
        return BatchTimer()
    }

    inline fun <T> batch(function: (BatchTimer) -> T): T {
        return BatchTimer().use(function)
    }

    @Suppress("NOTHING_TO_INLINE") // inline anyway
    private inline fun emit(
        step: Step,
        internalLatency: Duration,
        exception: Throwable? = null,
    ) {
        instrumentation.onStepTimed(
            newParameters(
                step = step,
                internalLatency = internalLatency,
                exception = exception,
            ),
        )
    }

    private fun newParameters(
        step: Step,
        internalLatency: Duration,
        exception: Throwable? = null,
    ): NadelInstrumentationTimingParameters {
        return NadelInstrumentationTimingParameters(
            step = step,
            internalLatency = internalLatency,
            exception = exception,
            context = userContext,
            instrumentationState = instrumentationState,
        )
    }

    inner class BatchTimer internal constructor() : Closeable {
        private val timings: MutableMap<Step, NadelParallelElapsedCalculator> = ConcurrentHashMap()

        private var exception: Throwable? = null

        inline fun <T> time(step: Step, function: () -> T): T {
            if (!isEnabled) {
                return function()
            }

            val start = ticker()

            return try {
                function()
            } catch (e: Throwable) {
                exception = e
                throw e
            } finally {
                val end = ticker()

                timings.computeIfAbsent(step) {
                    NadelParallelElapsedCalculator()
                }.submit(start, end)
            }
        }

        override fun close() {
            timings.forEach { (step, calculator) ->
                emit(step, calculator.calculate(), exception)
            }
        }

        override fun toString(): String {
            return "BatchTimer(timings=$timings)"
        }
    }
}
