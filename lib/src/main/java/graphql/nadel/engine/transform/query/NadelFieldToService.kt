package graphql.nadel.engine.transform.query

import graphql.nadel.NadelExecutableService
import graphql.nadel.NadelFieldInstructions
import graphql.nadel.Service
import graphql.nadel.ServiceLike
import graphql.nadel.engine.blueprint.IntrospectionService
import graphql.nadel.engine.blueprint.NadelExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelIntrospectionRunnerFactory
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelTypeRenameInstructions
import graphql.nadel.engine.util.copyWithChildren
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.hints.NadelExecutableServiceMigrationHint
import graphql.nadel.util.NamespacedUtil.isNamespacedField
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import graphql.schema.GraphQLSchema

internal class NadelFieldToService(
    querySchema: GraphQLSchema,
    private val executionBlueprint: NadelExecutionBlueprint,
    private val overallExecutionBlueprint: NadelOverallExecutionBlueprint,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
    private val dynamicServiceResolution: DynamicServiceResolution,
    private val executableServiceMigrationHint: NadelExecutableServiceMigrationHint,
) {
    private val publicIntrospectionService = IntrospectionService(querySchema, introspectionRunnerFactory)

    private val executablePublicIntrospectionService = NadelExecutableService(
        name = publicIntrospectionService.name,
        fieldInstructions = NadelFieldInstructions.Empty,
        typeInstructions = NadelTypeRenameInstructions.Empty,
        declaredOverallTypeNames = emptySet(),
        declaredUnderlyingTypeNames = emptySet(),
        serviceExecution = publicIntrospectionService.serviceExecution,
        service = publicIntrospectionService,
    )

    fun getServicesForTopLevelFields(
        query: ExecutableNormalizedOperation,
    ): List<NadelFieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            if (isNamespacedField(topLevelField)) {
                getServicePairsForNamespacedFields(topLevelField)
            } else {
                val serviceLike = if (executableServiceMigrationHint()) {
                    getExecutableService(topLevelField)
                } else {
                    getService(topLevelField)
                }

                listOf(NadelFieldAndService(topLevelField, serviceLike))
            }
        }
    }

    /**
     * Returns the dynamically resolved service for the field, if it is annotated with @dynamicServiceResolution,
     * otherwise returns the originalService.
     */
    fun resolveDynamicService(
        field: ExecutableNormalizedField,
        originalService: ServiceLike,
    ): ServiceLike {
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
            .groupBy { childField ->
                if (executableServiceMigrationHint()) {
                    getExecutableService(childField)
                } else {
                    getService(childField)
                }
            }
            .map { (serviceLike, childTopLevelFields) ->
                NadelFieldAndService(
                    field = topLevelField.copyWithChildren(childTopLevelFields),
                    serviceLike = serviceLike,
                )
            }
    }

    private fun getService(
        overallField: ExecutableNormalizedField,
    ): Service {
        if (overallField.name.startsWith("__")) {
            return publicIntrospectionService
        }

        val operationTypeName = overallField.objectTypeNames.single()
        val fieldCoordinates = makeFieldCoordinates(operationTypeName, overallField.name)

        return overallExecutionBlueprint.getServiceOwning(fieldCoordinates)
            ?: throw NadelFieldNotFoundException(overallField)
    }

    private fun getExecutableService(
        overallField: ExecutableNormalizedField,
    ): NadelExecutableService {
        if (overallField.name.startsWith("__")) {
            return executablePublicIntrospectionService
        }

        val operationTypeName = overallField.objectTypeNames.single()

        return executionBlueprint.fieldOwnershipMap.getByCoordinates(operationTypeName, overallField.name)
            ?: throw NadelFieldNotFoundException(overallField)
    }

    private fun isNamespacedField(field: ExecutableNormalizedField): Boolean {
        return isNamespacedField(field, executionBlueprint.engineSchema)
    }
}

data class NadelFieldAndService internal constructor(
    val field: ExecutableNormalizedField,
    internal val serviceLike: ServiceLike,
)
