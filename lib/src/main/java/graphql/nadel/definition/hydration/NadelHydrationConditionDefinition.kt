package graphql.nadel.definition.hydration

import graphql.language.InputObjectTypeDefinition
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.parseDefinition

data class NadelHydrationConditionDefinition(
    val result: NadelHydrationResultConditionDefinition,
) {
    companion object {
        val inputObjectDefinition = parseDefinition<InputObjectTypeDefinition>(
            """
              "Specify a condition for the hydration to activate"
              input NadelHydrationCondition {
                  result: NadelHydrationResultCondition!
              }
            """.trimIndent(),
        )

        fun from(
            conditionObject: JsonMap,
        ): NadelHydrationConditionDefinition? {
            @Suppress("UNCHECKED_CAST")
            val resultObject = conditionObject[Keyword.result] as Map<String, Any>?
                ?: return null

            return NadelHydrationConditionDefinition(
                result = NadelHydrationResultConditionDefinition.from(resultObject),
            )
        }
    }

    object Keyword {
        const val result = "result"
    }
}

/**
 * What field should match what value.
 */
data class NadelHydrationResultConditionDefinition(
    val pathToSourceField: List<String>,
    val predicate: NadelHydrationResultFieldPredicateDefinition,
) {
    companion object {
        val inputObjectDefinition = parseDefinition<InputObjectTypeDefinition>(
            // language=GraphQL
            """
                "Specify a condition for the hydration to activate based on the result"
                input NadelHydrationResultCondition {
                    sourceField: String!
                    predicate: NadelHydrationResultFieldPredicate!
                }
            """.trimIndent(),
        )

        fun from(resultObject: Map<String, Any>): NadelHydrationResultConditionDefinition {
            val sourceField = resultObject[Keyword.sourceField]!! as String

            @Suppress("UNCHECKED_CAST")
            val predicateObject = resultObject[Keyword.predicate]!! as Map<String, Any>

            return NadelHydrationResultConditionDefinition(
                pathToSourceField = sourceField.split("."),
                predicate = NadelHydrationResultFieldPredicateDefinition.from(predicateObject),
            )
        }
    }

    object Keyword {
        const val sourceField = "sourceField"
        const val predicate = "predicate"
    }
}

/**
 * How a given value must match.
 */
data class NadelHydrationResultFieldPredicateDefinition(
    val equals: Any?,
    val startsWith: String?,
    val matches: String?,
) {
    companion object {
        val inputValueDefinition = parseDefinition<InputObjectTypeDefinition>(
            // language=GraphQL
            """
                input NadelHydrationResultFieldPredicate @oneOf {
                    startsWith: String
                    equals: JSON
                    matches: String
                }
            """.trimIndent(),
        )

        fun from(predicateObject: Map<String, Any>): NadelHydrationResultFieldPredicateDefinition {
            return NadelHydrationResultFieldPredicateDefinition(
                equals = predicateObject[Keyword.equals],
                startsWith = predicateObject[Keyword.startsWith] as String?,
                matches = predicateObject[Keyword.matches] as String?,
            )
        }
    }

    object Keyword {
        const val equals = "equals"
        const val startsWith = "startsWith"
        const val matches = "matches"
    }
}
