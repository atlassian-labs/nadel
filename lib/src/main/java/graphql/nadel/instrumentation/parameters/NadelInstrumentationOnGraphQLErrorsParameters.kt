package graphql.nadel.instrumentation.parameters

import graphql.GraphQLError
import graphql.execution.instrumentation.InstrumentationState

data class NadelInstrumentationOnGraphQLErrorsParameters(
    val errors: List<GraphQLError>,
    val serviceName: String,
    private val instrumentationState: InstrumentationState?,
) {
    fun <T : InstrumentationState?> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }
}
