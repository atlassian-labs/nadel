package graphql.nadel.engine.transform.result

import graphql.GraphQLError
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.compiler.NadelExecutableNormalizedField

// todo: should be a value class one day… can't because of Java interop
data class NadelResultKey(val value: String)

sealed class NadelResultInstruction {
    data class Set(
        val subject: JsonNode,
        val key: NadelResultKey,
        val newValue: JsonNode?,
    ) : NadelResultInstruction() {
        constructor(
            subject: JsonNode,
            field: NadelExecutableNormalizedField,
            newValue: JsonNode?,
        ) : this(
            subject = subject,
            key = NadelResultKey(field.resultKey),
            newValue = newValue,
        )
    }

    data class Remove(
        val subject: JsonNode,
        val key: NadelResultKey,
    ) : NadelResultInstruction()

    data class AddError(
        val error: GraphQLError,
    ) : NadelResultInstruction()
}
