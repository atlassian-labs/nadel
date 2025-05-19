package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLSchema

sealed interface NadelAppliedDirectiveParentCoordinates : NadelSchemaMemberCoordinates {
    abstract override fun resolve(schema: GraphQLSchema): GraphQLDirectiveContainer?

    fun appliedDirective(name: String): NadelAppliedDirectiveCoordinates {
        return NadelAppliedDirectiveCoordinates(parent = this, name = name)
    }
}
