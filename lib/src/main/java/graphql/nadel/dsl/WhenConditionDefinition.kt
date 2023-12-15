package graphql.nadel.dsl


data class WhenConditionDefinition(
    val result: WhenConditionResultDefinition
)
data class WhenConditionResultDefinition(
    val sourceField: String,
    val predicate: WhenConditionPredicateDefinition,
)
data class WhenConditionPredicateDefinition(
    val equals: Any?,
    val startsWith: String?,
    val matches: Regex?
)



