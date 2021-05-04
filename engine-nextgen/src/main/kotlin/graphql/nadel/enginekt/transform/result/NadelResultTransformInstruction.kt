package graphql.nadel.enginekt.transform.result

import graphql.nadel.enginekt.transform.result.json.JsonNodePath

sealed class NadelResultInstruction

interface NadelResultInstructionWithSubject {
    val subjectPath: JsonNodePath

    val subjectKey: String
        get() = subjectPath.segments.last().value as String
}

data class NadelResultSetInstruction(
    override val subjectPath: JsonNodePath,
    val newValue: Any?,
) : NadelResultInstruction(), NadelResultInstructionWithSubject

data class NadelResultRemoveInstruction(
    override val subjectPath: JsonNodePath,
) : NadelResultInstruction(), NadelResultInstructionWithSubject

data class NadelResultCopyInstruction(
    override val subjectPath: JsonNodePath,
    val destinationPath: JsonNodePath,
) : NadelResultInstruction(), NadelResultInstructionWithSubject {
    val destinationKey: String
        get() = destinationPath.segments.last().value as String
}
