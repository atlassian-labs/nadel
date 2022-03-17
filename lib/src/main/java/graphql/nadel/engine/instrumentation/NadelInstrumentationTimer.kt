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

        return function()
            .also {
                val end = System.nanoTime()
                context.onCompleted(Duration.ofNanos(end - start), null)
            }
    }
}
