package graphql.nadel.enginekt.blueprint

import graphql.schema.FieldCoordinates

sealed class GraphQLArtificialField {
    abstract val location: FieldCoordinates
}

data class GraphQLHydrationField(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
) : GraphQLArtificialField()

data class GraphQLBatchHydrationField(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val batchSize: Int,
    val objectIdentifier: String,
    val matchByIndex: Boolean,
) : GraphQLArtificialField()

class GraphQLPullField(
    override val location: FieldCoordinates,
    val pathToSourceField: List<String>,
) : GraphQLArtificialField()
