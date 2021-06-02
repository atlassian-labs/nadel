package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgument
import graphql.schema.FieldCoordinates

sealed class NadelFieldInstruction {
    abstract val location: FieldCoordinates
}

interface NadelGenericHydrationInstruction {
    val sourceService: Service
    val pathToSourceField: List<String>
    val sourceFieldArguments: List<NadelHydrationArgument>
}

data class NadelHydrationFieldInstruction(
    override val location: FieldCoordinates,
    override val sourceService: Service,
    override val pathToSourceField: List<String>,
    override val sourceFieldArguments: List<NadelHydrationArgument>,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelBatchHydrationFieldInstruction(
    override val location: FieldCoordinates,
    override val sourceService: Service,
    override val pathToSourceField: List<String>,
    override val sourceFieldArguments: List<NadelHydrationArgument>,
    val batchSize: Int,
    val batchHydrationMatchStrategy: NadelBatchHydrationMatchStrategy,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelDeepRenameFieldInstruction(
    override val location: FieldCoordinates,
    val pathToSourceField: List<String>,
) : NadelFieldInstruction()

class NadelRenameFieldInstruction(
    override val location: FieldCoordinates,
    val underlyingName: String,
) : NadelFieldInstruction()
