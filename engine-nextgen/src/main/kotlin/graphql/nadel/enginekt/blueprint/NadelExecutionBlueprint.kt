package graphql.nadel.enginekt.blueprint

import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

data class NadelExecutionBlueprint(
    val underlyingFields: Map<FieldCoordinates, NadelFieldRenameInstruction>,
    val underlyingTypes: Map<String, NadelTypeRenameInstruction>,
    val fieldInstructions: Map<FieldCoordinates, NadelInstruction>,
)

operator fun <T> Map<FieldCoordinates, T>.get(key: NormalizedField): T? {
    return this[makeFieldCoordinates(key.objectType, key.fieldDefinition)]
}
