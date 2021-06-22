package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.schema.NadelDirectives
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQuery

class SplitFieldsByUnderlyingService {

    fun execute(query: NormalizedQuery, overallExecutionBlueprint: NadelOverallExecutionBlueprint): List<FieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            val isNamespacedField = topLevelField.getOneFieldDefinition(overallExecutionBlueprint.schema)
                .getDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name) != null
            when {
                isNamespacedField -> {
                    val namespacedChildFieldsByService = topLevelField.children
                        .groupBy {

                            return@groupBy overallExecutionBlueprint.coordinatesToService[
                                    makeFieldCoordinates(
                                        it.getOneObjectType(overallExecutionBlueprint.schema),
                                        it.getOneFieldDefinition(overallExecutionBlueprint.schema)
                                    )
                            ]!!
                        }

                    namespacedChildFieldsByService.map { entry ->
                        val childTopLevelFields = entry.value
                        val topLevelFieldForService = topLevelField.copyWithChildren(childTopLevelFields)
                        val service = entry.key
                        FieldAndService(topLevelFieldForService, service)
                    }
                }
                else -> listOf(
                    FieldAndService(
                        field = topLevelField,
                        service = overallExecutionBlueprint.coordinatesToService[
                                makeFieldCoordinates(
                                    topLevelField.getOneObjectType(overallExecutionBlueprint.schema),
                                    topLevelField.getOneFieldDefinition(overallExecutionBlueprint.schema)
                                )]!!
                    )
                )
            }
        }
    }
}

data class FieldAndService(
    val field: NormalizedField,
    val service: Service,
)
