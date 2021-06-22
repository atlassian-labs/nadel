package graphql.nadel.enginekt.schema

import graphql.language.ObjectTypeDefinition
import graphql.nadel.OperationKind
import graphql.nadel.Service
import graphql.nadel.enginekt.util.getOperationType
import graphql.nadel.enginekt.util.getOperationTypes
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.mapFrom
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

data class NadelFieldInfos(
    val queryTopLevelFields: Map<FieldCoordinates, Service>,
    val mutationTopLevelFields: Map<FieldCoordinates, Service>,
    val subscriptionTopLevelFields: Map<FieldCoordinates, Service>,
) {
    fun getFieldInfo(operationKind: OperationKind, topLevelFieldName: FieldCoordinates): Service? {
        return when (operationKind) {
            OperationKind.QUERY -> queryTopLevelFields[topLevelFieldName]
            OperationKind.MUTATION -> mutationTopLevelFields[topLevelFieldName]
            OperationKind.SUBSCRIPTION -> subscriptionTopLevelFields[topLevelFieldName]
        }
    }

    companion object {
        fun create(services: List<Service>, overallSchema: GraphQLSchema): NadelFieldInfos {
            return NadelFieldInfos(
                getServiceForTopLevelField(services, overallSchema, OperationKind.QUERY),
                getServiceForTopLevelField(services, overallSchema, OperationKind.MUTATION),
                getServiceForTopLevelField(services, overallSchema, OperationKind.SUBSCRIPTION),
            )
        }

        private fun getServiceForTopLevelField(
            services: List<Service>,
            overallSchema: GraphQLSchema,
            operationKind: OperationKind,
        ): Map<FieldCoordinates, Service> {

            val allNamespacedTypesByName = overallSchema.getOperationType(operationKind)
                ?.fieldDefinitions
                .orEmpty()
                .filter { it.getDirectives("namespaced").isNotEmpty() }
                .map { it.type as GraphQLObjectType }
                .associateBy { it.name }

            val fieldCoordinatesToServicePairs: List<Pair<FieldCoordinates, Service>> =
                services.flatMap forService@{ service ->
                    // Definition registry is the service's AST contributions to the overall schema
                    val pairsForNamespacedFields = service.definitionRegistry
                        .getDefinitions(ObjectTypeDefinition::class.java)
                        .mapNotNull { typeDefinition ->
                            val namespacedType: GraphQLObjectType = allNamespacedTypesByName[typeDefinition.name]
                                ?: return@mapNotNull null

                            typeDefinition.fieldDefinitions
                                .map {
                                    makeFieldCoordinates(
                                        namespacedType.name,
                                        it.name
                                    ) to service
                                }
                        }
                        .flatten()

                    val operationTypes = service.definitionRegistry.getOperationTypes(operationKind)
                    val pairsForTopLevelFields = operationTypes.flatMap { operationType ->
                        operationType.fieldDefinitions.mapNotNull { fieldDef ->
                            when {
                                fieldDef.hasDirective("namespaced") -> null
                                else -> makeFieldCoordinates(
                                    operationType.name,
                                    fieldDef.name
                                ) to service

                            }
                        }
                    }

                    return@forService pairsForTopLevelFields + pairsForNamespacedFields
                }
            return mapFrom(fieldCoordinatesToServicePairs)
        }
    }
}
