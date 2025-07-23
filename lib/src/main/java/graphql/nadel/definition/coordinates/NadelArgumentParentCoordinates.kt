package graphql.nadel.definition.coordinates

sealed interface NadelArgumentParentCoordinates : NadelSchemaMemberCoordinates {
    fun argument(name: String): NadelArgumentCoordinates {
        return NadelArgumentCoordinates(parent = this, name = name)
    }
}
