package graphql.nadel.enginekt.blueprint

sealed class GraphQLArtificialField

data class GraphQLHydrationField(
    val name: String,
    val sourceService: String,
    val pathToSourceField: List<String>,
) : GraphQLArtificialField()

data class GraphQLBatchHydrationField(
    val name: String,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val batchSize: Int,
    val objectIdentifier: String,
    val matchByIndex: Boolean,
) : GraphQLArtificialField()

class GraphQLPullField(
    val name: String,
    val path: List<String>,
) : GraphQLArtificialField()
