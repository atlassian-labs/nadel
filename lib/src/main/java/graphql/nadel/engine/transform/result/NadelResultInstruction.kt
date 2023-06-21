package graphql.nadel.engine.transform.result

import graphql.GraphQLError
import graphql.nadel.engine.transform.result.json.JsonNode

// todo: should be a value class one day… can't because of Java interop
data class NadelResultKey(val value: String)

sealed class NadelResultInstruction {
    data class Set(
        val subject: JsonNode,
        val key: NadelResultKey,
        val newValue: JsonNode?,
    ) : NadelResultInstruction()

    data class Remove(
        val subject: JsonNode,
        val key: NadelResultKey,
    ) : NadelResultInstruction()

    data class AddError(
        val error: GraphQLError,
    ) : NadelResultInstruction()
}
