package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation

internal class NadelFieldToService(
    private val overallExecutionBlueprint: NadelOverallExecutionBlueprint,
) {
    fun getServicesForTopLevelFields(query: ExecutableNormalizedOperation): List<NadelFieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            if (isNamespacedField(topLevelField)) {
                getServicePairsForNamespacedFields(topLevelField)
            } else {
                listOf(getServicePairFor(field = topLevelField))
            }
        }
    }

    private fun getServicePairsForNamespacedFields(
        topLevelField: ExecutableNormalizedField,
    ): List<NadelFieldAndService> {
        return topLevelField.children
            .map { childField ->
                val service = getService(childField)
                val topLevelFieldForService = topLevelField.copyWithChildren(
                    children = listOf(childField),
                )
                NadelFieldAndService(topLevelFieldForService, service)
            }
    }

    private fun getServicePairFor(field: ExecutableNormalizedField): NadelFieldAndService {
        return NadelFieldAndService(
            field = field,
            service = getService(field),
        )
    }

    private fun getService(overallField: ExecutableNormalizedField): Service {
        val operationTypeName = overallField.objectTypeNames.single()
        val fieldCoordinates = makeFieldCoordinates(operationTypeName, overallField.name)
        return overallExecutionBlueprint.getService(fieldCoordinates)
            ?: error("Unable to find service for field at: $fieldCoordinates")
    }

    private fun isNamespacedField(field: ExecutableNormalizedField): Boolean {
        return NadelNamespacedFields.isNamespacedField(field, overallExecutionBlueprint.schema)
    }
}

data class NadelFieldAndService(
    val field: ExecutableNormalizedField,
    val service: Service,
)
