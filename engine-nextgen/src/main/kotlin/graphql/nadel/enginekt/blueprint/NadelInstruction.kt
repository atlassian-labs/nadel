package graphql.nadel.enginekt.blueprint

import graphql.nadel.enginekt.blueprint.hydration.HydrationArgument
import graphql.schema.FieldCoordinates

sealed class NadelInstruction {
    abstract val location: FieldCoordinates
}

data class NadelHydrationInstruction(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val arguments: List<HydrationArgument>,
) : NadelInstruction()

data class NadelBatchHydrationInstruction(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val arguments: List<HydrationArgument>,
    val batchSize: Int,
    val objectIdentifier: String,
    val matchByIndex: Boolean,
) : NadelInstruction()

class NadelDeepRenameInstruction(
    override val location: FieldCoordinates,
    val pathToSourceField: List<String>,
) : NadelInstruction()
