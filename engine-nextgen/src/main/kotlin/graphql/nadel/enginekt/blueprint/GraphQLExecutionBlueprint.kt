package graphql.nadel.enginekt.blueprint

import graphql.schema.FieldCoordinates

data class GraphQLExecutionBlueprint(
    val underlyingFields: Map<FieldCoordinates, GraphQLUnderlyingField>,
    val underlyingTypes: Map<String, GraphQLUnderlyingType>,
    val artificialFields: Map<FieldCoordinates, GraphQLArtificialField>,
)
