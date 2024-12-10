package graphql.nadel.instrumentation.parameters

import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.nadel.NadelUserContext
import graphql.normalized.ExecutableNormalizedOperation
import graphql.schema.GraphQLSchema

/**
 * Parameters sent to [graphql.nadel.instrumentation.NadelInstrumentation] methods
 */
data class NadelInstrumentationExecuteOperationParameters(
    val normalizedOperation: ExecutableNormalizedOperation,
    val document: Document,
    val graphQLSchema: GraphQLSchema,
    val variables: MutableMap<String, Any?>,
    val operationDefinition: OperationDefinition,
    private val instrumentationState: InstrumentationState?,
    private val context: NadelUserContext?,
) {
    fun <T : NadelUserContext> getContext(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return context as T?
    }

    fun <T : InstrumentationState?> getInstrumentationState(): T? {
        @Suppress("UNCHECKED_CAST") // trust the caller
        return instrumentationState as T?
    }
}
