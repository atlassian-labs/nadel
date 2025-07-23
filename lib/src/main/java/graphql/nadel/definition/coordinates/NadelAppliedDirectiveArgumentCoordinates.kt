package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLSchema

data class NadelAppliedDirectiveArgumentCoordinates(
    override val parent: NadelAppliedDirectiveCoordinates,
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelChildCoordinates {
    override val level: Int = parent.level + 1

    override fun resolve(schema: GraphQLSchema): GraphQLAppliedDirectiveArgument {
        return parent.resolve(schema).getArgument(name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
