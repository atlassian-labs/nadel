package graphql.nadel.definition.coordinates

sealed interface NadelTopLevelDefinitionCoordinates : NadelSchemaMemberCoordinates {
    override val level: Int
        get() = 0
}
