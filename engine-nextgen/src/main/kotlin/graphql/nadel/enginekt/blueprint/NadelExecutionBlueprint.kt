package graphql.nadel.enginekt.blueprint

import graphql.schema.FieldCoordinates

data class NadelExecutionBlueprint(
    val underlyingFields: Map<FieldCoordinates, NadelRenameInstruction>,
    val underlyingTypes: Map<String, NadelUnderlyingType>,
    val artificialFields: Map<FieldCoordinates, NadelInstruction>,
)
