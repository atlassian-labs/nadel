package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState

class NadelInstrumentationIsTimingEnabledParameters(
    private val instrumentationState: InstrumentationState?,
    private val context: Any?,
    val operationName: String?,
) {
    fun <T : InstrumentationState> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }

    fun <T> getContext(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return context as T?
    }
}
