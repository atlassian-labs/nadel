package graphql.nadel.instrumentation.parameters

import graphql.ExecutionInput
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.schema.GraphQLSchema

/**
 * Parameters sent to [graphql.nadel.instrumentation.NadelInstrumentation] methods
 */
data class NadelInstrumentationQueryValidationParameters(
    val executionInput: ExecutionInput,
    val document: Document,
    val schema: GraphQLSchema,
    private val instrumentationState: InstrumentationState?,
    private val context: Any?,
){
    fun <T> getContext(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return context as T?
    }

    fun <T : InstrumentationState> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }
}
