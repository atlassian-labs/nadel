package graphql.nadel.enginekt.blueprint

import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

data class NadelExecutionBlueprint(
    val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    val typeInstructions: Map<String, NadelTypeRenameInstruction>,
)

fun <T> Map<FieldCoordinates, T>.getForField(
    key: NormalizedField,
): Map<FieldCoordinates, T> {
    return mapFrom(
        key.objectTypeNames.asSequence()
            .map {
                makeFieldCoordinates(it, key.fieldName)
            }
            .mapNotNull {
                it to (this[it] ?: return@mapNotNull null)
            }
            .toList(),
    )
}
