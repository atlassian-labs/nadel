package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInput
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.schema.FieldCoordinates

sealed class NadelFieldInstruction {
    abstract val location: FieldCoordinates
}

interface NadelGenericHydrationInstruction {
    val actorService: Service
    val queryPathToActorField: QueryPath
    val actorInputValues: List<NadelHydrationActorInput>
}

data class NadelHydrationFieldInstruction(
    override val location: FieldCoordinates,
    override val actorService: Service,
    override val queryPathToActorField: QueryPath,
    override val actorInputValues: List<NadelHydrationActorInput>,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelBatchHydrationFieldInstruction(
    override val location: FieldCoordinates,
    override val actorService: Service,
    override val queryPathToActorField: QueryPath,
    override val actorInputValues: List<NadelHydrationActorInput>,
    val batchSize: Int,
    val batchHydrationMatchStrategy: NadelBatchHydrationMatchStrategy,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelDeepRenameFieldInstruction(
    override val location: FieldCoordinates,
    val queryPathToField: QueryPath,
) : NadelFieldInstruction()

data class NadelRenameFieldInstruction(
    override val location: FieldCoordinates,
    val underlyingName: String,
) : NadelFieldInstruction() {
    val overallName: String get() = location.fieldName
}
