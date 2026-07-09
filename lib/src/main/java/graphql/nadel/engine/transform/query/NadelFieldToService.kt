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
        val topLevelFields = query.topLevelFields

        // Resolve the owning service for each non-namespaced root field, and bucket the ones that
        // are eligible for batching by service. LinkedHashMap preserves first-occurrence order.
        val fieldToService = HashMap<ExecutableNormalizedField, Service>()
        val batchedByService = LinkedHashMap<Service, MutableList<ExecutableNormalizedField>>()
        for (topLevelField in topLevelFields) {
            if (isNamespacedField(topLevelField)) {
                continue
            }
            val service = getService(topLevelField)
            fieldToService[topLevelField] = service
            if (canBatchRootField(topLevelField, service, executionHints)) {
                batchedByService.getOrPut(service) { mutableListOf() }.add(topLevelField)
            }
        }

        // Emit entries in query order. A batched service's root fields are emitted as a single
        // entry (one service call) at the position of the service's first batchable root field;
        // everything else keeps its own single-field entry, exactly as before.
        val result = mutableListOf<NadelFieldAndService>()
        val emittedBatches = HashSet<Service>()
        for (topLevelField in topLevelFields) {
            if (isNamespacedField(topLevelField)) {
                result += getServicePairsForNamespacedFields(topLevelField, executionHints)
                continue
            }

            val service = fieldToService.getValue(topLevelField)
            val batchedFields = batchedByService[service]
            if (batchedFields != null && topLevelField in batchedFields) {
                if (emittedBatches.add(service)) {
                    result += NadelFieldAndService(field = batchedFields, service = service)
                }
            } else {
                result += NadelFieldAndService(field = listOf(topLevelField), service = service)
            }
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
        field: List<ExecutableNormalizedField>,
        originalService: Service,
    ): Service {
        return if (dynamicServiceResolution.needsDynamicServiceResolution(field.first())) {
            dynamicServiceResolution.resolveServiceForField(field.first())
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
                    field = listOf(topLevelField.copyWithChildren(childTopLevelFields)),
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
    val field: List<ExecutableNormalizedField>,
    val service: Service,
)

