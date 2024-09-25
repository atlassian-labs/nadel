package graphql.nadel.engine.transform.partition

import graphql.language.ScalarValue
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInputValueDefinition

/**
 * TODO: Add documentation
 */
interface NadelPartitionTransformHook {

    /**
     * This hook allows you to provide a path to the partition point for a given field.
     * If null is returned, no partitioning will be done for the field.
     */
    fun getPathToPartitionPoint(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): List<String>? {
        return null
    }

    /**
     * This hook allows you to provide a partition key for a given scalar value.
     * Arguments with the same partition key will be sent on the same request.
     */
    fun getPartitionKeyExtractor(): ((ScalarValue<*>, GraphQLInputValueDefinition) -> String?) {
        return {_, _ -> null}
    }
}
