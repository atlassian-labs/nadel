package graphql.nadel.engine.transform.query

import graphql.introspection.Introspection
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.engine.blueprint.IntrospectionService
import graphql.nadel.engine.blueprint.NadelIntrospectionRunnerFactory
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.util.copyWithChildren
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.util.NamespacedUtil.isNamespacedField
import graphql.nadel.util.NamespacedUtil.serviceOwnsNamespacedField
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import graphql.schema.GraphQLSchema

internal class NadelFieldToService(
    private val querySchema: GraphQLSchema,
    private val overallExecutionBlueprint: NadelOverallExecutionBlueprint,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
    private val dynamicServiceResolution: DynamicServiceResolution,
    private val services: Map<String, Service>,
) {
    private val introspectionService = IntrospectionService(querySchema, introspectionRunnerFactory)

    fun getServicesForTopLevelFields(
        query: ExecutableNormalizedOperation,
        executionHints: NadelExecutionHints,
    ): List<NadelFieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            if (isNamespacedField(topLevelField)) {
                getServicePairsForNamespacedFields(topLevelField, executionHints)
            } else {
                listOf(getServicePairFor(field = topLevelField))
            }
        }
    }

    /**
     * Returns the dynamically resolved service for the field, if it is annotated with @dynamicServiceResolution,
     * otherwise returns the originalService.
     */
    fun resolveDynamicService(
        field: ExecutableNormalizedField,
        originalService: Service,
    ): Service {
        return if (dynamicServiceResolution.needsDynamicServiceResolution(field)) {
            dynamicServiceResolution.resolveServiceForField(field)
        } else {
            originalService
        }
    }

    private fun getServicePairsForNamespacedFields(
        topLevelField: ExecutableNormalizedField,
        executionHints: NadelExecutionHints,
    ): List<NadelFieldAndService> {
        return topLevelField.children
            .groupBy { childField ->
                getServiceForNamespacedField(childField, executionHints)
            }
            .map { (service, childTopLevelFields) ->
                NadelFieldAndService(
                    field = topLevelField.copyWithChildren(childTopLevelFields),
                    service = service,
                )
            }
    }

    private fun getServicePairFor(field: ExecutableNormalizedField): NadelFieldAndService {
        return NadelFieldAndService(
            field = field,
            service = getService(field),
        )
    }

    private fun getServiceForNamespacedField(
        overallField: ExecutableNormalizedField,
        executionHints: NadelExecutionHints,
    ): Service {
        if (overallField.name == Introspection.TypeNameMetaFieldDef.name) {
            val namespaceTypeName = overallField.objectTypeNames.single()

            return if (executionHints.newResultMergerAndNamespacedTypename()) {
                introspectionService
            } else {
                services.values.first { service ->
                    serviceOwnsNamespacedField(namespaceTypeName, service)
                }
            }
        }

        return getService(overallField)
    }

    private fun getService(overallField: ExecutableNormalizedField): Service {
        if (overallField.name.startsWith("__")) {
            return introspectionService
        }

        val operationTypeName = overallField.objectTypeNames.single()
        val fieldCoordinates = makeFieldCoordinates(operationTypeName, overallField.name)
        return overallExecutionBlueprint.getServiceOwning(fieldCoordinates)
            ?: error("Unable to find service for field at: $fieldCoordinates")
    }

    private fun isNamespacedField(field: ExecutableNormalizedField): Boolean {
        return isNamespacedField(field, overallExecutionBlueprint.engineSchema)
    }
}

data class NadelFieldAndService(
    val field: ExecutableNormalizedField,
    val service: Service,
)

