package graphql.nadel

import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.schema.FieldCoordinates

/**
 * This contains details about a service hydration call when a [ServiceExecution] is invoked.
 */
data class ServiceExecutionHydrationDetails(
    internal val instruction: NadelGenericHydrationInstruction,
    val timeout: Int,
    val batchSize: Int,
    val hydrationSourceService: Service,
    val hydrationVirtualField: FieldCoordinates,
    val hydrationBackingField: FieldCoordinates,
    val fieldPath: List<String>,
)
