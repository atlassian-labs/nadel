package graphql.nadel.engine.transform.partition

import graphql.language.ScalarValue
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInputValueDefinition

interface NadelPartitionTransformHook {

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

    fun getPartitionKeyExtractor(): ((ScalarValue<*>, GraphQLInputValueDefinition) -> String?) {
        return {_, _ -> null}
    }
}
