package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema

data class NadelScalarCoordinates(
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelTypeCoordinates,
    NadelAppliedDirectiveParentCoordinates {
    override val kind = NadelCoordinateKind.Scalar

    override fun resolve(schema: GraphQLSchema): GraphQLScalarType? {
        return schema.getType(name) as GraphQLScalarType?
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
