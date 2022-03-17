package graphql.nadel.instrumentation.parameters

import graphql.ExecutionInput
import graphql.schema.GraphQLSchema

/**
 * Parameters sent to [graphql.nadel.instrumentation.NadelInstrumentation] methods
 */
data class NadelInstrumentationCreateStateParameters(
    val schema: GraphQLSchema,
    val executionInput: ExecutionInput,
)
