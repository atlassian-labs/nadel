package graphql.nadel.engine.transform.result

import graphql.GraphQLError
import graphql.nadel.engine.transform.result.json.JsonNode

@JvmInline
value class ResultKey(val value: String)

sealed class NadelResultInstruction {
    data class Set(
        val subject: JsonNode,
        val key: ResultKey,
        val newValue: Any?,
    ) : NadelResultInstruction()

    data class Remove(
        val subject: JsonNode,
        val key: ResultKey,
    ) : NadelResultInstruction()

    data class AddError(
        val error: GraphQLError,
    ) : NadelResultInstruction()
}
