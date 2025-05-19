package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLSchema

sealed interface NadelFieldContainerCoordinates : NadelSchemaMemberCoordinates {
    abstract override fun resolve(schema: GraphQLSchema): GraphQLFieldsContainer?

    fun field(name: String): NadelFieldCoordinates {
        return NadelFieldCoordinates(parent = this, name = name)
    }
}
