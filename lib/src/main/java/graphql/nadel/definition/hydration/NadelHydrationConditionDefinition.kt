package graphql.nadel.definition.hydration

import graphql.nadel.engine.util.JsonMap

data class NadelHydrationConditionDefinition(
    val result: NadelHydrationResultConditionDefinition,
) {
    companion object {
        internal fun from(
            conditionObject: JsonMap,
        ): NadelHydrationConditionDefinition? {
            @Suppress("UNCHECKED_CAST")
            val result = conditionObject[Keyword.result] as Map<String, Any>?
                ?: return null

            val sourceField = result[Keyword.sourceField]!! as String

            @Suppress("UNCHECKED_CAST")
            val predicate = result[Keyword.predicate]!! as Map<String, Any>

            return NadelHydrationConditionDefinition(
                result = NadelHydrationResultConditionDefinition(
                    pathToSourceField = sourceField.split("."),
                    predicate = NadelHydrationConditionPredicateDefinition(
                        equals = predicate[Keyword.equals],
                        startsWith = predicate[Keyword.startsWith] as String?,
                        matches = predicate[Keyword.matches] as String?,
                    ),
                ),
            )
        }
    }

    object Keyword {
        const val result = "result"
        const val sourceField = "sourceField"
        const val predicate = "predicate"
        const val equals = "equals"
        const val startsWith = "startsWith"
        const val matches = "matches"
    }
}

/**
 * What field should match what value.
 */
data class NadelHydrationResultConditionDefinition(
    val pathToSourceField: List<String>,
    val predicate: NadelHydrationConditionPredicateDefinition,
)

/**
 * How a given value must match.
 */
data class NadelHydrationConditionPredicateDefinition(
    val equals: Any?,
    val startsWith: String?,
    val matches: String?,
)
