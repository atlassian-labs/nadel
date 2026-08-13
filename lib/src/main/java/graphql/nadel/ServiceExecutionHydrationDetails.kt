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
) {
    private var sharedConsumers: List<NadelServiceExecutionHydrationConsumerDetails>? = null

    /**
     * All hydration consumers represented by this service call.
     *
     * Isolated calls contain one element. A coalesced call retains the first consumer in the
     * legacy singular properties above and exposes every consumer here.
     *
     * This metadata deliberately lives outside the data-class constructor to preserve its
     * existing JVM descriptors. The generated [copy] function therefore retains only the legacy
     * singular properties and the copied value reports a single consumer.
     */
    val consumerDetails: List<NadelServiceExecutionHydrationConsumerDetails>
        get() = sharedConsumers ?: listOf(toConsumerDetails())

    internal fun withConsumers(
        consumers: List<NadelServiceExecutionHydrationConsumerDetails>,
    ): ServiceExecutionHydrationDetails {
        require(consumers.isNotEmpty())
        sharedConsumers = consumers.toList()
        return this
    }

    internal fun toConsumerDetails(): NadelServiceExecutionHydrationConsumerDetails {
        return NadelServiceExecutionHydrationConsumerDetails(
            instruction = instruction,
            hydrationSourceService = hydrationSourceService,
            hydrationVirtualField = hydrationVirtualField,
            hydrationBackingField = hydrationBackingField,
            fieldPath = fieldPath,
        )
    }
}

data class NadelServiceExecutionHydrationConsumerDetails internal constructor(
    internal val instruction: NadelGenericHydrationInstruction,
    val hydrationSourceService: Service,
    val hydrationVirtualField: FieldCoordinates,
    val hydrationBackingField: FieldCoordinates,
    val fieldPath: List<String>,
)
