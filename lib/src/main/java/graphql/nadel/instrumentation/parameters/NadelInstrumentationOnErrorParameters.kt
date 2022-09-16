package graphql.nadel.instrumentation.parameters

open class NadelInstrumentationOnErrorParameters(
    val message: String,
    val exception: Throwable,
)

class OnServiceExecutionErrorParameters(
    message: String,
    exception: Throwable,
    val errorData: ExecutionErrorData
) : NadelInstrumentationOnErrorParameters(
    message, exception
)

data class ExecutionErrorData(
    val executionId: String
)

