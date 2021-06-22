package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.schema.NadelDirectives
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQuery
import graphql.schema.GraphQLSchema

class NadelFieldToServiceRouter {

    fun execute(query: NormalizedQuery, overallExecutionBlueprint: NadelOverallExecutionBlueprint): List<FieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            when {
                NadelNamespacedFields.isNamespacedField(topLevelField, overallExecutionBlueprint.schema) -> {
                    val namespacedChildFieldsByService = topLevelField.children
                        .groupBy {
                            overallExecutionBlueprint.coordinatesToService[
                                    makeFieldCoordinates(
                                        it.getOneObjectType(overallExecutionBlueprint.schema),
                                        it.getOneFieldDefinition(overallExecutionBlueprint.schema)
                                    )
                            ]!!
                        }

                    namespacedChildFieldsByService.map { (service, childTopLevelFields) ->
                        val topLevelFieldForService = topLevelField.copyWithChildren(childTopLevelFields)
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

object NadelNamespacedFields {
    fun isNamespacedField(field: NormalizedField, schema: GraphQLSchema): Boolean {
        return field.getOneFieldDefinition(schema)
            .getDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name) != null
    }
}
