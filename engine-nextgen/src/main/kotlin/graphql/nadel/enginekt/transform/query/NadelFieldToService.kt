package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation

class NadelFieldToService(private val overallExecutionBlueprint: NadelOverallExecutionBlueprint) {

    fun getServicesForTopLevelFields(query: ExecutableNormalizedOperation): List<NadelFieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            when {
                NadelNamespacedFields.isNamespacedField(topLevelField, overallExecutionBlueprint.schema) -> {
                    val namespacedChildFieldsByService: Map<Service, List<ExecutableNormalizedField>> =
                        topLevelField.children
                            .groupBy(::getService)

                    namespacedChildFieldsByService.map { (service, childTopLevelFields) ->
                        val topLevelFieldForService = topLevelField.copyWithChildren(childTopLevelFields)
                        NadelFieldAndService(topLevelFieldForService, service)
                    }
                }
                else -> listOf(
                    NadelFieldAndService(
                        field = topLevelField,
                        service = getService(topLevelField)
                    )
                )
            }
        }
    }

    private fun getService(overallField: ExecutableNormalizedField): Service {
        val operationTypeName = overallField.objectTypeNames.single()
        val fieldCoordinates = makeFieldCoordinates(operationTypeName, overallField.name)
        return overallExecutionBlueprint.getService(overallField)
            ?: error("Unable to find service for field at: $fieldCoordinates")
    }
}

data class NadelFieldAndService(
    val field: ExecutableNormalizedField,
    val service: Service,
)
