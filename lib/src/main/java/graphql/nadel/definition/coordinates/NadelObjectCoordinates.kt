package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

data class NadelObjectCoordinates(
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelTypeCoordinates,
    NadelImplementingTypeCoordinates,
    NadelAppliedDirectiveParentCoordinates,
    NadelFieldContainerCoordinates {
    override fun resolve(schema: GraphQLSchema): GraphQLObjectType? {
        return schema.getType(name) as GraphQLObjectType?
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
