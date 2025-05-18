package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLSchema

data class NadelInputObjectCoordinates(
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelTypeCoordinates,
    NadelAppliedDirectiveParentCoordinates {
    override fun resolve(schema: GraphQLSchema): GraphQLInputObjectType? {
        return schema.getType(name) as GraphQLInputObjectType?
    }

    fun field(name: String): NadelInputObjectFieldCoordinates {
        return NadelInputObjectFieldCoordinates(parent = this, name = name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
