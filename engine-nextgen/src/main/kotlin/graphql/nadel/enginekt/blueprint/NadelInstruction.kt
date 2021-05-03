package graphql.nadel.enginekt.blueprint

import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgument
import graphql.schema.FieldCoordinates

sealed class NadelInstruction {
    abstract val location: FieldCoordinates
}

data class NadelHydrationInstruction(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val arguments: List<NadelHydrationArgument>,
) : NadelInstruction()

data class NadelBatchHydrationInstruction(
    override val location: FieldCoordinates,
    val sourceService: String,
    val pathToSourceField: List<String>,
    val arguments: List<NadelHydrationArgument>,
    val batchSize: Int,
    val batchHydrationMatchStrategy: NadelBatchHydrationMatchStrategy,
) : NadelInstruction()

class NadelDeepRenameInstruction(
    override val location: FieldCoordinates,
    val pathToSourceField: List<String>,
) : NadelInstruction()
