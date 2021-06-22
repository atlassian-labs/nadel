package graphql.nadel.enginekt.transform.query

import graphql.nadel.OperationKind
import graphql.nadel.Service
import graphql.nadel.enginekt.schema.FieldCoordinatesToUnderlyingServiceMapping
import graphql.nadel.enginekt.util.copyWithChildren
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQuery
import graphql.schema.GraphQLSchema

class SplitFieldsByUnderlyingService {

    fun execute(query: NormalizedQuery, schema: GraphQLSchema, fieldInfos: FieldCoordinatesToUnderlyingServiceMapping, operationKind: OperationKind): List<FieldAndService> {
        return query.topLevelFields
            .flatMap { topLevelField ->
                val isNamespacedField = topLevelField.getOneFieldDefinition(schema).getDirective("namespaced") != null
                when {
                    isNamespacedField -> {
                        val namespacedChildFieldsByService = topLevelField.children
                            .groupBy {
                                fieldInfos.getService(
                                    makeFieldCoordinates(
                                        it.getOneObjectType(schema),
                                        it.getOneFieldDefinition(schema)
                                    ),
                                    operationKind
                                )!!
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
                            service = fieldInfos.getService(
                                makeFieldCoordinates(
                                    topLevelField.getOneObjectType(schema),
                                    topLevelField.getOneFieldDefinition(schema)
                                ), operationKind
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
