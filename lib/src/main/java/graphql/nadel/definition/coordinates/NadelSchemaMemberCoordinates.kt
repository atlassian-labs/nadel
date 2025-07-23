package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLSchemaElement

sealed interface NadelSchemaMemberCoordinates : Comparable<NadelSchemaMemberCoordinates?> {
    val name: String

    val level: Int

    val parentOrNull: NadelSchemaMemberCoordinates?
        get() = (this as? NadelChildCoordinates)?.parent

    val parents: Sequence<NadelSchemaMemberCoordinates>
        get() = generateSequence(seed = parentOrNull) { it.parentOrNull }

    fun resolve(schema: GraphQLSchema): GraphQLSchemaElement?

    fun isChildOf(other: NadelSchemaMemberCoordinates): Boolean {
        return parents.any { it == other }
    }

    /**
     * Should effectively be compared alphabetically by level.
     */
    override fun compareTo(other: NadelSchemaMemberCoordinates?): Int {
        val compareLevel = compareValues(level, other?.level)

        return if (compareLevel == 0) {
            val compareParents = compareValues(parentOrNull, other?.parentOrNull)
            if (compareParents == 0) {
                val compareKind = compareValues(javaClass.simpleName, other?.javaClass?.simpleName)
                if (compareKind == 0) {
                    compareValues(name, other?.name)
                } else {
                    compareKind
                }
            } else {
                compareParents
            }
        } else {
            compareLevel
        }
    }

    companion object {
        internal fun toHumanReadableString(coordinates: NadelSchemaMemberCoordinates): String {
            return (sequenceOf(coordinates) + coordinates.parents)
                .map { c ->
                    val name = c.name
                    val kind = getHumanReadableKind(c)
                    "$name ($kind)"
                }
                .toList()
                .asReversed()
                .joinToString(separator = ".")
        }

        private fun getHumanReadableKind(coordinates: NadelSchemaMemberCoordinates): String {
            return when (coordinates) {
                is NadelAppliedDirectiveArgumentCoordinates -> "AppliedDirectiveArgument"
                is NadelAppliedDirectiveCoordinates -> "AppliedDirective"
                is NadelArgumentCoordinates -> "Argument"
                is NadelEnumCoordinates -> "Enum"
                is NadelEnumValueCoordinates -> "EnumValue"
                is NadelFieldCoordinates -> "Field"
                is NadelInputObjectCoordinates -> "InputObject"
                is NadelInputObjectFieldCoordinates -> "InputObjectField"
                is NadelInterfaceCoordinates -> "Interface"
                is NadelObjectCoordinates -> "Object"
                is NadelScalarCoordinates -> "Scalar"
                is NadelUnionCoordinates -> "Union"
                is NadelDirectiveCoordinates -> "Directive"
            }
        }
    }
}
