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
    querySchema: GraphQLSchema,
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
        // Feature flag: when root-field batching is globally disabled, keep the original behaviour
        // of one entry (i.e. one service call) per root field.
        if (!executionHints.batchRootFields()) {
            return query.topLevelFields.flatMap { topLevelField ->
                if (isNamespacedField(topLevelField)) {
                    getServicePairsForNamespacedFields(topLevelField, executionHints)
                } else {
                    listOf(NadelFieldAndService(fields = listOf(topLevelField), service = getService(topLevelField)))
                }
            }
        }

        // Group batch-eligible root fields per service into a single entry (one service call).
        val result = mutableListOf<NadelFieldAndService>()
        val batchedByService = LinkedHashMap<Service, MutableList<ExecutableNormalizedField>>()
        for (topLevelField in query.topLevelFields) {
            if (isNamespacedField(topLevelField)) {
                result += getServicePairsForNamespacedFields(topLevelField, executionHints)
                continue
            }

            val service = getService(topLevelField)
            if (canBatchRootField(topLevelField, service, executionHints)) {
                batchedByService.getOrPut(service) { mutableListOf() }.add(topLevelField)
            } else {
                result += NadelFieldAndService(fields = listOf(topLevelField), service = service)
            }
        }

        batchedByService.forEach { (service, batchedFields) ->
            result += NadelFieldAndService(fields = batchedFields, service = service)
        }

        return result
    }

    /**
     * Root fields are only batched into a single service call when the service opts in via
     * [NadelExecutionHints.batchRootFields].
     */
    private fun canBatchRootField(
        field: ExecutableNormalizedField,
        service: Service,
        executionHints: NadelExecutionHints,
    ): Boolean {
        if (!executionHints.batchRootFields(service)) {
            return false
        }
        if (field.name.startsWith("__")) {
            return false
        }
        return !dynamicServiceResolution.needsDynamicServiceResolution(field)
    }

    /**
     * Returns the dynamically resolved service for the field, if it is annotated with @dynamicServiceResolution,
     * otherwise returns the originalService.
     */
    fun resolveDynamicService(
        fields: List<ExecutableNormalizedField>,
        originalService: Service,
    ): Service {
        // Dynamically-resolved fields are never batched (see canBatchRootField), so dynamic
        // resolution only applies to single-field entries; batched entries keep the original service.
        val field = fields.singleOrNull() ?: return originalService
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
                    fields = listOf(topLevelField.copyWithChildren(childTopLevelFields)),
                    service = service,
                )
            }
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
    val fields: List<ExecutableNormalizedField>,
    val service: Service,
)

