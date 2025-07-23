package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLSchema

data class NadelDirectiveCoordinates(
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelTopLevelDefinitionCoordinates,
    NadelArgumentParentCoordinates {
    override fun resolve(schema: GraphQLSchema): GraphQLDirective? {
        return schema.getDirective(name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
