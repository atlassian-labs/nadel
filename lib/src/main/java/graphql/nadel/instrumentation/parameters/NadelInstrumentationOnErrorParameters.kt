package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState

data class NadelInstrumentationOnErrorParameters(
    val message: String,
    val exception: Throwable,
    val errorType: ErrorType,
    val errorData: ErrorData,
    private val instrumentationState: InstrumentationState?,
) {
    fun <T : InstrumentationState?> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }
}

sealed interface ErrorData {
    data class ServiceExecutionErrorData(
        val executionId: String,
        val serviceName: String,
    ) : ErrorData
}

enum class ErrorType {
    ServiceExecutionError
}
