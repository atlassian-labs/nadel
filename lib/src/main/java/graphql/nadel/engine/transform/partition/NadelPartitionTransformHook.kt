package graphql.nadel.engine.transform.partition

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceLike
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.normalized.ExecutableNormalizedField

/**
 * This hook allows you to provide custom partitioning logic for fields.
 */
interface NadelPartitionTransformHook {

    /**
     * This hook allows you to provide a custom context based on the characteristics of the field being partitioned.
     *
     * This context will be passed to the partition key extractor function so you can use it to determine the partition key.
     *
     * You can use this to check custom directives on the field, or any other details of the field definition.
     *
     * If `null` is returned, the field will not be partitioned.
     *
     */
    fun getFieldPartitionContext(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: ServiceLike,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): NadelFieldPartitionContext?

    /**
     * This hook allows you to provide a partition key for a given scalar value.
     * Arguments with the same partition key will be sent on the same request.
     */
    fun getPartitionKeyExtractor(): NadelPartitionKeyExtractor

    /**
     * This hook allows you to perform some non-side effect action on the field partitions (like logging, etc.).
     * This is called before the field partitions calls are executed, and whether the partition requests are successful or not.
     */
    fun onPartition(executionContext: NadelExecutionContext, fieldPartitions: Map<String, ExecutableNormalizedField>) {
        // no-op
    }
}
