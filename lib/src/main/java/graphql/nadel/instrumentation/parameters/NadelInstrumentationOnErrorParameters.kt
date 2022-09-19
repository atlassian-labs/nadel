package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState

data class NadelInstrumentationOnErrorParameters(
    val message: String,
    val exception: Throwable,
    val errorData: Any?,
    private val instrumentationState: InstrumentationState?,
) {
    fun <T : InstrumentationState?> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }
}

data class OnServiceExecutionErrorData(
    val executionId: String,
    val serviceName: String
)
