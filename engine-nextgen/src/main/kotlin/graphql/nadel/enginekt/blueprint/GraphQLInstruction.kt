package graphql.nadel.enginekt.blueprint

import graphql.nadel.enginekt.blueprint.hydration.HydrationArgument
import graphql.schema.FieldCoordinates

sealed class GraphQLInstruction {
    abstract val location: FieldCoordinates
}

data class GraphQLHydrationInstruction(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val arguments: List<HydrationArgument>,
) : GraphQLInstruction()

data class GraphQLBatchHydrationInstruction(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val arguments: List<HydrationArgument>,
    val batchSize: Int,
    val objectIdentifier: String,
    val matchByIndex: Boolean,
) : GraphQLInstruction()

class GraphQLDeepRenameInstruction(
    override val location: FieldCoordinates,
    val pathToSourceField: List<String>,
) : GraphQLInstruction()
