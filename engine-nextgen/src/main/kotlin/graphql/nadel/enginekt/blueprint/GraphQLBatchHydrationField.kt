package graphql.nadel.enginekt.blueprint

data class GraphQLBatchHydrationField(
    val name: String,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val batchSize: Int,
    val objectIdentifier: String,
    val matchByIndex: Boolean,
)
