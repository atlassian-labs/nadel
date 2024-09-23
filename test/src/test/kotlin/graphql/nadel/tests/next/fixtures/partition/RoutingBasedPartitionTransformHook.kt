package graphql.nadel.tests.next.fixtures.partition

import graphql.language.ArrayValue
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
    override fun getPathToPartitionPoint(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, graphql.nadel.Service>,
        service: graphql.nadel.Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): List<String>? {
        val fieldDef = overallField.getFieldDefinitions(executionBlueprint.engineSchema).first()
        val routingDirective = fieldDef.getDirective("routing") ?: return null
        val pathToSplitPoint = routingDirective.getArgument("pathToSplitPoint")
            ?.toAppliedArgument()
            ?.argumentValue
            ?.value as ArrayValue?

        return pathToSplitPoint?.values?.map { it as StringValue }?.map { it.value }
    }

    override fun getPartitionKeyExtractor(): (ScalarValue<*>, GraphQLInputValueDefinition) -> String? {
        return { scalarValue, _ ->
            val stringValue = scalarValue as StringValue

            stringValue.value.split(":")[1]
        }
    }
}
