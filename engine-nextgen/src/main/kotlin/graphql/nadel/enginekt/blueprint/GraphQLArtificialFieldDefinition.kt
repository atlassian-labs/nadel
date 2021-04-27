package graphql.nadel.enginekt.blueprint

import graphql.schema.FieldCoordinates

sealed class GraphQLArtificialFieldDefinition {
    abstract val location: FieldCoordinates
}

data class GraphQLHydrationFieldDefinition(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
) : GraphQLArtificialFieldDefinition()

data class GraphQLBatchHydrationFieldDefinition(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val batchSize: Int,
    val objectIdentifier: String,
    val matchByIndex: Boolean,
) : GraphQLArtificialFieldDefinition()

class GraphQLPullFieldDefinition(
    override val location: FieldCoordinates,
    val pathToSourceField: List<String>,
) : GraphQLArtificialFieldDefinition()
