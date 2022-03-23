package graphql.nadel.engine.transform.query

import graphql.introspection.Introspection
import graphql.nadel.Service
import graphql.nadel.engine.introspection.IntrospectionServiceFactory
import graphql.nadel.engine.introspection.NadelIntrospectionRunnerFactory
import graphql.nadel.engine.transform.query.NadelNamespacedFields.isNamespacedField
import graphql.nadel.engine.util.AnyImplementingTypeDefinition
import graphql.nadel.engine.util.copyWithChildren
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.toMapStrictly
import graphql.nadel.util.NamespacedUtil.serviceOwnsNamespacedField
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

internal class NadelFieldToService(
    private val engineSchema: GraphQLSchema,
    querySchema: GraphQLSchema,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
    private val dynamicServiceResolution: DynamicServiceResolution,
    private val services: Map<String, Service>,
) {
    private val introspectionService = IntrospectionServiceFactory.make(querySchema, introspectionRunnerFactory)

    private val topLevelFieldToService: Map<FieldCoordinates, Service> = services.values
        .asSequence()
        .flatMap { service ->
            service.definitionRegistry
                .definitions
                .asSequence()
                .filterIsInstance<AnyImplementingTypeDefinition>()
                .flatMap { fieldContainer ->
                    fieldContainer.fieldDefinitions
                        .map { fieldDef ->
                            makeFieldCoordinates(fieldContainer.name, fieldDef.name)
                        }
                }
                .map {
                    it to service
                }
        }
        .toMapStrictly()

    fun getServicesForTopLevelFields(query: ExecutableNormalizedOperation): List<NadelFieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            if (isNamespacedField(topLevelField)) {
                getServicePairsForNamespacedFields(topLevelField)
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
    ): List<NadelFieldAndService> {
        return topLevelField.children
            .groupBy(::getServiceForNamespacedField)
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

    private fun getServiceForNamespacedField(overallField: ExecutableNormalizedField): Service {
        if (overallField.name == Introspection.TypeNameMetaFieldDef.name) {
            // TODO: replace this logic with internal handling via IntrospectionService
            // See https://github.com/atlassian-labs/nadel/pull/324
            val operationTypeName = overallField.objectTypeNames.single()
            return services.values.first { service ->
                serviceOwnsNamespacedField(operationTypeName, service)
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
        return topLevelFieldToService[fieldCoordinates]
            ?: throw IllegalStateException("Field is not mapped to any service")
    }

    private fun isNamespacedField(field: ExecutableNormalizedField): Boolean {
        return isNamespacedField(field, engineSchema)
    }
}

data class NadelFieldAndService(
    val field: ExecutableNormalizedField,
    val service: Service,
)
