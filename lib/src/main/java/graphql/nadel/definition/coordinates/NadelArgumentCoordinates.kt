package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLSchema

data class NadelArgumentCoordinates(
    override val parent: NadelArgumentParentCoordinates,
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelChildCoordinates,
    NadelAppliedDirectiveParentCoordinates,
    NadelInputCoordinates {
    override val level: Int = parent.level + 1

    override fun resolve(schema: GraphQLSchema): GraphQLArgument? {
        return when (parent) {
            is NadelDirectiveCoordinates -> parent.resolve(schema)?.getArgument(name)
            is NadelFieldCoordinates -> parent.resolve(schema)?.getArgument(name)
        }
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
