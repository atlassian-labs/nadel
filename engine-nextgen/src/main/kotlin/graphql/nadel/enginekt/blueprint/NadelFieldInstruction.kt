package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition

sealed class NadelFieldInstruction {
    abstract val location: FieldCoordinates
}

interface NadelGenericHydrationInstruction {
    val hydratedFieldDef: GraphQLFieldDefinition
    val actorService: Service
    val queryPathToActorField: NadelQueryPath
    val actorFieldDef: GraphQLFieldDefinition
    val actorInputValueDefs: List<NadelHydrationActorInputDef>
    val serviceExecutionHydrationDetails: ServiceExecutionHydrationDetails
}

data class NadelHydrationFieldInstruction(
    override val location: FieldCoordinates,
    override val hydratedFieldDef: GraphQLFieldDefinition,
    override val actorService: Service,
    override val queryPathToActorField: NadelQueryPath,
    override val actorFieldDef: GraphQLFieldDefinition,
    override val actorInputValueDefs: List<NadelHydrationActorInputDef>,
    override val serviceExecutionHydrationDetails: ServiceExecutionHydrationDetails,
    val hydrationStrategy: NadelHydrationStrategy,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelBatchHydrationFieldInstruction(
    override val location: FieldCoordinates,
    override val hydratedFieldDef: GraphQLFieldDefinition,
    override val actorService: Service,
    override val queryPathToActorField: NadelQueryPath,
    override val actorFieldDef: GraphQLFieldDefinition,
    override val actorInputValueDefs: List<NadelHydrationActorInputDef>,
    override val serviceExecutionHydrationDetails: ServiceExecutionHydrationDetails,
    val batchSize: Int,
    val batchHydrationMatchStrategy: NadelBatchHydrationMatchStrategy,
) : NadelFieldInstruction(), NadelGenericHydrationInstruction

data class NadelDeepRenameFieldInstruction(
    override val location: FieldCoordinates,
    val queryPathToField: NadelQueryPath,
) : NadelFieldInstruction()

data class NadelRenameFieldInstruction(
    override val location: FieldCoordinates,
    val underlyingName: String,
) : NadelFieldInstruction() {
    val overallName: String get() = location.fieldName
}
