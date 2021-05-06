package graphql.nadel.enginekt.blueprint

import graphql.schema.FieldCoordinates

data class NadelExecutionBlueprint(
    val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    val typeInstructions: Map<String, NadelTypeRenameInstruction>,
)
