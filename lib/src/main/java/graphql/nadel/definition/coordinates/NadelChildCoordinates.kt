package graphql.nadel.definition.coordinates

sealed interface NadelChildCoordinates : NadelSchemaMemberCoordinates {
    val parent: NadelSchemaMemberCoordinates
}
