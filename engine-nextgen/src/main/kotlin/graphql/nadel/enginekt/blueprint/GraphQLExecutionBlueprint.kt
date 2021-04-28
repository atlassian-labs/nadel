package graphql.nadel.enginekt.blueprint

import graphql.schema.FieldCoordinates

data class GraphQLExecutionBlueprint(
    val underlyingFields: Map<FieldCoordinates, NadelRenameInstruction>,
    val underlyingTypes: Map<String, GraphQLUnderlyingType>,
    val artificialFields: Map<FieldCoordinates, NadelInstruction>,
)
