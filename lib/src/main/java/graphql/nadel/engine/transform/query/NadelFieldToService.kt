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
import graphql.nadel.engine.compiler.NadelExecutableNormalizedField
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
        return query.topLevelFields.flatMap { graphqlJavaTopLevelField ->
            // Engine seam: convert graphql-java's ExecutableNormalizedField tree (produced by the operation
            // factory) into Nadel's forked NadelExecutableNormalizedField, which the rest of the engine and the
            // forked compiler operate on. This is where the forcePrintAsUnconditional flag lives from here on.
            val topLevelField =
                NadelExecutableNormalizedField.fromExecutableNormalizedField(graphqlJavaTopLevelField)
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
        field: NadelExecutableNormalizedField,
        originalService: Service,
    ): Service {
        return if (dynamicServiceResolution.needsDynamicServiceResolution(field)) {
            dynamicServiceResolution.resolveServiceForField(field)
        } else {
            originalService
        }
    }

    private fun getServicePairsForNamespacedFields(
        topLevelField: NadelExecutableNormalizedField,
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

    private fun getServicePairFor(field: NadelExecutableNormalizedField): NadelFieldAndService {
        return NadelFieldAndService(
            field = field,
            service = getService(field),
        )
    }

    private fun getServiceForNamespacedField(
        overallField: NadelExecutableNormalizedField,
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

    private fun getService(overallField: NadelExecutableNormalizedField): Service {
        if (overallField.name.startsWith("__")) {
            return introspectionService
        }

        val operationTypeName = overallField.objectTypeNames.single()
        val fieldCoordinates = makeFieldCoordinates(operationTypeName, overallField.name)
        return overallExecutionBlueprint.getServiceOwning(fieldCoordinates)
            ?: error("Unable to find service for field at: $fieldCoordinates")
    }

    private fun isNamespacedField(field: NadelExecutableNormalizedField): Boolean {
        return isNamespacedField(field, overallExecutionBlueprint.engineSchema)
    }
}

data class NadelFieldAndService(
    val field: NadelExecutableNormalizedField,
    val service: Service,
)

