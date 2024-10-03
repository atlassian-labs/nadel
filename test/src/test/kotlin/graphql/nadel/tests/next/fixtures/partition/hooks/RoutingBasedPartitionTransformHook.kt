package graphql.nadel.tests.next.fixtures.partition.hooks

import graphql.language.ScalarValue
import graphql.language.StringValue
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInputValueDefinition

class RoutingBasedPartitionTransformHook : NadelPartitionTransformHook {
    override fun getFieldPartitionContext(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, graphql.nadel.Service>,
        service: graphql.nadel.Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): Unit {
        return Unit
    }

    override fun getPartitionKeyExtractor(): (ScalarValue<*>, GraphQLInputValueDefinition, Any?) -> String? {
        return { scalarValue, _, _ ->
            if (scalarValue !is StringValue) {
                null
            } else {
                if (scalarValue.value.contains(":").not()) {
                    null
                } else {
                    scalarValue.value.split(":")[1]
                }
            }
        }
    }
}
