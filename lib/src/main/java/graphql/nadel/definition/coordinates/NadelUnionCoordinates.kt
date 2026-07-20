package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType

data class NadelUnionCoordinates(
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelTypeCoordinates,
    NadelAppliedDirectiveParentCoordinates {
    override val kind = NadelCoordinateKind.Union

    override fun resolve(schema: GraphQLSchema): GraphQLUnionType? {
        return schema.getType(name) as GraphQLUnionType?
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
