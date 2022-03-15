package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState

data class NadelInstrumentationTimingParameters(
    val step: Step,
    private val context: Any?,
    private val instrumentationState: InstrumentationState?,
) {
    fun <T> getContext(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return context as T
    }

    fun <T : InstrumentationState?> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }

    enum class Step {
        ExecutionPlanning,
        QueryTransforming,
        ResultTransforming,
    }
}
