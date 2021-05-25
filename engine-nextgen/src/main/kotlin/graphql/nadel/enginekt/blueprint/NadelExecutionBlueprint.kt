package graphql.nadel.enginekt.blueprint

import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

data class NadelExecutionBlueprint(
    val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    val typeInstructions: Map<String, NadelTypeRenameInstruction>,
)

fun <T> Map<FieldCoordinates, T>.getForField(key: NormalizedField): T? {
    return key.objectTypeNames.mapNotNull {
        this[makeFieldCoordinates(it, key.fieldName)]
    }.singleOrNull()
}
