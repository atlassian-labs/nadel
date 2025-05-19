package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLSchema

data class NadelInterfaceCoordinates(
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelTypeCoordinates,
    NadelImplementingTypeCoordinates,
    NadelAppliedDirectiveParentCoordinates,
    NadelFieldContainerCoordinates {
    override fun resolve(schema: GraphQLSchema): GraphQLInterfaceType? {
        return schema.getType(name) as GraphQLInterfaceType?
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
