package graphql.nadel.enginekt.blueprint

data class GraphQLHydrationField(
    val name: String,
    val sourceService: String,
    val pathToSourceField: List<String>,
)
