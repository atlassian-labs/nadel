package graphql.nadel.enginekt.blueprint

import graphql.nadel.enginekt.util.filterValuesOfType
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

data class NadelExecutionBlueprint(
    val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    val typeInstructions: Map<String, NadelTypeRenameInstruction>,
)

fun <T> Map<FieldCoordinates, T>.getForField(
    field: NormalizedField,
): Map<FieldCoordinates, T> {
    return mapFrom(
        field.objectTypeNames.asSequence()
            .map {
                makeFieldCoordinates(it, field.fieldName)
            }
            .mapNotNull {
                it to (this[it] ?: return@mapNotNull null)
            }
            .toList(),
    )
}

inline fun <reified T : NadelFieldInstruction> Map<FieldCoordinates, NadelFieldInstruction>.getInstructionsOfTypeForField(
    field: NormalizedField,
): Map<FieldCoordinates, T> {
    return getForField(field).filterValuesOfType()
}
