package graphql.nadel.enginekt.blueprint

import graphql.schema.FieldCoordinates

data class NadelExecutionBlueprint(
    val underlyingFields: Map<FieldCoordinates, NadelFieldRenameInstruction>,
    val underlyingTypes: Map<String, NadelTypeRenameInstruction>,
    val artificialFields: Map<FieldCoordinates, NadelInstruction>,
)
