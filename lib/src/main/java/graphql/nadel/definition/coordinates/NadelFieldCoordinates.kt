package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema

data class NadelFieldCoordinates(
    override val parent: NadelFieldContainerCoordinates,
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelChildCoordinates,
    NadelArgumentParentCoordinates,
    NadelAppliedDirectiveParentCoordinates {
    override val level: Int = parent.level + 1

    override fun resolve(schema: GraphQLSchema): GraphQLFieldDefinition? {
        return parent.resolve(schema)?.getField(name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
