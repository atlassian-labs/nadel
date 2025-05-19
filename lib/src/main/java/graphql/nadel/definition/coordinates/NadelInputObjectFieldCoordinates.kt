package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLSchema

data class NadelInputObjectFieldCoordinates(
    override val parent: NadelInputObjectCoordinates,
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelChildCoordinates,
    NadelAppliedDirectiveParentCoordinates,
    NadelInputCoordinates {
    override val level: Int = parent.level + 1

    override fun resolve(schema: GraphQLSchema): GraphQLInputObjectField? {
        return parent.resolve(schema)?.getField(name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
