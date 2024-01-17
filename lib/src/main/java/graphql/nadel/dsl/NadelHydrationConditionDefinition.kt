package graphql.nadel.dsl

data class NadelHydrationConditionDefinition(
    val result: NadelHydrationResultConditionDefinition,
)

data class NadelHydrationResultConditionDefinition(
    val pathToSourceField: List<String>,
    val predicate: NadelHydrationConditionPredicateDefinition,
)

data class NadelHydrationConditionPredicateDefinition(
    val equals: Any?,
    val startsWith: String?,
    val matches: Regex?,
)
