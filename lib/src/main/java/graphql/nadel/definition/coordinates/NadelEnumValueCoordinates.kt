package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLSchema

data class NadelEnumValueCoordinates(
    override val parent: NadelEnumCoordinates,
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelChildCoordinates,
    NadelAppliedDirectiveParentCoordinates {
    override val level: Int = parent.level + 1

    override fun resolve(schema: GraphQLSchema): GraphQLEnumValueDefinition? {
        return parent.resolve(schema)?.getValue(name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
