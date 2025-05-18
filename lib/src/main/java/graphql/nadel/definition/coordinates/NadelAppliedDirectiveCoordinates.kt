package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLSchema

data class NadelAppliedDirectiveCoordinates(
    override val parent: NadelAppliedDirectiveParentCoordinates,
    override val name: String,
) : NadelSchemaMemberCoordinates,
    NadelChildCoordinates {
    override val level: Int = parent.level + 1

    override fun resolve(schema: GraphQLSchema): Nothing {
        throw UnsupportedOperationException()
    }

    fun argument(name: String): NadelAppliedDirectiveArgumentCoordinates {
        return NadelAppliedDirectiveArgumentCoordinates(parent = this, name = name)
    }

    override fun toString(): String {
        return NadelSchemaMemberCoordinates.toHumanReadableString(this)
    }
}
