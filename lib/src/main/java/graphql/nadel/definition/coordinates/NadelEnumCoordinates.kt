package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLSchema

data class NadelEnumCoordinates(
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelTypeCoordinates,
    NadelAppliedDirectiveParentCoordinates {
    override fun resolve(schema: GraphQLSchema): GraphQLEnumType? {
        return schema.getType(name) as GraphQLEnumType?
    }

    fun enumValue(name: String): NadelEnumValueCoordinates {
        return NadelEnumValueCoordinates(parent = this, name = name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
