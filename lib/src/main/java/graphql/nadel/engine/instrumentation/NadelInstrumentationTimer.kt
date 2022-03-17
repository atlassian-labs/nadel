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
        val parameters = NadelInstrumentationTimingParameters(step, userContext, instrumentationState)
        val context = instrumentation.beginTiming(parameters)
        val start = System.nanoTime()

        try {
            return function()
                .also {
                    context.onCompleted(getDurationSince(start), null)
                }
        } catch (e: Throwable) {
            context.onCompleted(getDurationSince(start), e)
            throw e
        }
    }

    @Suppress("NOTHING_TO_INLINE") // inline anyway
    private inline fun getDurationSince(startNanos: Long): Duration? {
        val end = System.nanoTime()
        return Duration.ofNanos(end - startNanos)
    }
}
