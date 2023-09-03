package graphql.nadel

import graphql.schema.FieldCoordinates

/**
 * This contains details about a service hydration call when a [ServiceExecution] is invoked.
 */
data class ServiceExecutionHydrationDetails(
    val timeout: Int,
    val batchSize: Int,
    val hydrationSourceService: Service,
    val hydrationSourceField: FieldCoordinates,
    val hydrationActorField: FieldCoordinates,
    val hydrationPath: List<String>,
)
