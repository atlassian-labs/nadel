package graphql.nadel.enginekt.transform.result

import graphql.nadel.enginekt.transform.result.json.JsonNodePath

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
}

interface NadelResultInstructionWithSubject {
    val subjectPath: JsonNodePath

    val subjectKey: String
        get() = subjectPath.segments.last().value as String
}
