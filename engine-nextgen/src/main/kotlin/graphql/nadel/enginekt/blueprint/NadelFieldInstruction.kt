package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgument
import graphql.schema.FieldCoordinates

sealed class NadelFieldInstruction {
    abstract val location: FieldCoordinates
}

data class NadelHydrationFieldInstruction(
    override val location: FieldCoordinates,
    val sourceService: Service,
    val pathToSourceField: List<String>,
    val arguments: List<NadelHydrationArgument>,
) : NadelFieldInstruction()

data class NadelBatchHydrationFieldInstruction(
    override val location: FieldCoordinates,
    val sourceService: Service,
    val pathToSourceField: List<String>,
    val arguments: List<NadelHydrationArgument>,
    val batchSize: Int,
    val batchHydrationMatchStrategy: NadelBatchHydrationMatchStrategy,
) : NadelFieldInstruction()

data class NadelDeepRenameFieldInstruction(
    override val location: FieldCoordinates,
    val pathToSourceField: List<String>,
) : NadelFieldInstruction()

class NadelRenameFieldInstruction(
    override val location: FieldCoordinates,
    val underlyingName: String,
) : NadelFieldInstruction()
