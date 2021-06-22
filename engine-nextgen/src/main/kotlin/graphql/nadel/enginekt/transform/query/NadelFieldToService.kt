package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.schema.NadelDirectives
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQuery
import graphql.schema.GraphQLSchema

class NadelFieldToService {

    fun getServicesForTopLevelFields(query: NormalizedQuery, overallExecutionBlueprint: NadelOverallExecutionBlueprint): List<FieldAndService> {
        return query.topLevelFields.flatMap { topLevelField ->
            when {
                NadelNamespacedFields.isNamespacedField(topLevelField, overallExecutionBlueprint.schema) -> {
                    val namespacedChildFieldsByService: Map<Service, List<NormalizedField>> =
                        topLevelField.children
                            .groupBy {
                                val parentType = it.getOneObjectType(overallExecutionBlueprint.schema)
                                val graphQLFieldDefinition = it.getOneFieldDefinition(overallExecutionBlueprint.schema)
                                overallExecutionBlueprint.getService(parentType, graphQLFieldDefinition)!!
                            }

                    namespacedChildFieldsByService.map { (service, childTopLevelFields) ->
                        val topLevelFieldForService = topLevelField.copyWithChildren(childTopLevelFields)
                        FieldAndService(topLevelFieldForService, service)
                    }
                }
                else -> listOf(
                    FieldAndService(
                        field = topLevelField,
                        service = overallExecutionBlueprint.getService(
                            topLevelField.getOneObjectType(overallExecutionBlueprint.schema),
                            topLevelField.getOneFieldDefinition(overallExecutionBlueprint.schema)
                        )!!
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
