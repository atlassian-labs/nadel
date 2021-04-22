package graphql.nadel.enginekt.schema

import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.nadel.OperationKind
import graphql.nadel.Service
import graphql.nadel.enginekt.util.mapFrom
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema

class GraphQLFieldInfos(overallSchema: GraphQLSchema, services: List<Service>) {
    val queryTopLevelFields = getFieldInfos(overallSchema, services, OperationKind.QUERY)
    val mutationTopLevelFields = getFieldInfos(overallSchema, services, OperationKind.MUTATION)
    val subscriptionTopLevelFields = getFieldInfos(overallSchema, services, OperationKind.SUBSCRIPTION)

    fun getFieldInfo(operationKind: OperationKind, topLevelFieldName: String): GraphQLFieldInfo? {
        return when (operationKind) {
            OperationKind.QUERY -> queryTopLevelFields[topLevelFieldName]
            OperationKind.MUTATION -> mutationTopLevelFields[topLevelFieldName]
            OperationKind.SUBSCRIPTION -> subscriptionTopLevelFields[topLevelFieldName]
        }
    }

    private fun getFieldInfos(
        overallSchema: GraphQLSchema,
        services: List<Service>,
        operationKind: OperationKind,
    ): Map<String, GraphQLFieldInfo> {
        val overallOperationType = when (operationKind) {
            OperationKind.QUERY -> overallSchema.queryType
            OperationKind.MUTATION -> overallSchema.mutationType
            OperationKind.SUBSCRIPTION -> overallSchema.subscriptionType
        }

        return mapFrom(
            services.flatMap { service ->
                val serviceOperationTypes: List<ObjectTypeDefinition> = when (operationKind) {
                    OperationKind.QUERY -> service.definitionRegistry.queryType
                    OperationKind.MUTATION -> service.definitionRegistry.mutationType
                    OperationKind.SUBSCRIPTION -> service.definitionRegistry.subscriptionType
                }
                serviceOperationTypes.flatMap { serviceOperationType ->
                    serviceOperationType.fieldDefinitions.map { fieldDefinition: FieldDefinition ->
                        val field: GraphQLFieldDefinition = overallOperationType.getField(fieldDefinition.name)

                        field.name to GraphQLFieldInfo(service, operationKind, field)
                    }
                }
            }
        )
    }
}

