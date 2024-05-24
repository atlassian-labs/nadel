package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState

data class NadelInstrumentationOnExceptionParameters(
    val exception: Throwable,
    /**
     * Which service was being executed, if known.
     */
    val serviceName: String?,
    private val instrumentationState: InstrumentationState?,
) {
    fun <T : InstrumentationState?> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }
}

