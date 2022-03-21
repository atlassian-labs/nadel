package graphql.nadel.engine.transform.result

import graphql.GraphQLError
import graphql.nadel.engine.transform.result.json.JsonNodePath

sealed class NadelResultInstruction {
    data class Set(
        override val subjectPath: JsonNodePath,
        val newValue: Any?,
    ) : NadelResultInstruction(), NadelResultInstructionWithSubject

    data class Remove(
        override val subjectPath: JsonNodePath,
    ) : NadelResultInstruction(), NadelResultInstructionWithSubject

    data class Copy(
        override val subjectPath: JsonNodePath,
        val destinationPath: JsonNodePath,
    ) : NadelResultInstruction(), NadelResultInstructionWithSubject {
        val destinationKey: String
            get() = destinationPath.segments.last().value as String
    }

    data class AddError(
        val error: GraphQLError,
    ) : NadelResultInstruction()
}

interface NadelResultInstructionWithSubject {
    val subjectPath: JsonNodePath

    val subjectKey: String
        get() = subjectPath.segments.last().value as String
}
