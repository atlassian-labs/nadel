package graphql.nadel.instrumentation.parameters

import graphql.ExecutionInput
import graphql.execution.instrumentation.InstrumentationState
import graphql.schema.GraphQLSchema

/**
 * Parameters sent to [graphql.nadel.instrumentation.NadelInstrumentation] methods
 */
data class NadelInstrumentationQueryExecutionParameters(
    val executionInput: ExecutionInput,
    val query: String,
    val operation: String?,
    private val context: Any?,
    val variables: Map<String?, Any?>,
    private val instrumentationState: InstrumentationState?,
    val schema: GraphQLSchema,
) {
    constructor(
        executionInput: ExecutionInput,
        schema: GraphQLSchema,
        instrumentationState: InstrumentationState?,
    ) : this(
        executionInput,
        query = executionInput.query,
        operation = executionInput.operationName,
        context = executionInput.context,
        variables = executionInput.variables,
        instrumentationState,
        schema,
    )

    fun <T> getContext(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return context as T?
    }

    fun <T : InstrumentationState> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }
}
