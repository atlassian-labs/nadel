package graphql.nadel.enginekt.schema

import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.nadel.OperationKind
import graphql.nadel.Service
import graphql.nadel.enginekt.util.getOperationType
import graphql.nadel.enginekt.util.getOperationTypes
import graphql.nadel.enginekt.util.mapFrom
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema

data class NadelFieldInfos(
    val queryTopLevelFields: Map<String, NadelFieldInfo>,
    val mutationTopLevelFields: Map<String, NadelFieldInfo>,
    val subscriptionTopLevelFields: Map<String, NadelFieldInfo>,
) {
    fun getFieldInfo(operationKind: OperationKind, topLevelFieldName: String): NadelFieldInfo? {
        return when (operationKind) {
            OperationKind.QUERY -> queryTopLevelFields[topLevelFieldName]
            OperationKind.MUTATION -> mutationTopLevelFields[topLevelFieldName]
            OperationKind.SUBSCRIPTION -> subscriptionTopLevelFields[topLevelFieldName]
        }
    }

    companion object {
        fun create(services: List<Service>, overallSchema: GraphQLSchema): NadelFieldInfos {
            val nadelFieldInfos = NadelFieldInfos(
                getInfosFromServices(services, overallSchema, OperationKind.QUERY),
                getInfosFromServices(services, overallSchema, OperationKind.MUTATION),
                getInfosFromServices(services, overallSchema, OperationKind.SUBSCRIPTION),
            )
            return nadelFieldInfos
        }

        private fun getInfosFromServices(
            services: List<Service>,
            overallSchema: GraphQLSchema,
            operationKind: OperationKind,
        ): Map<String, NadelFieldInfo> {

            val namespacedTypes: List<GraphQLOutputType> = overallSchema.getOperationType(operationKind)
                ?.fieldDefinitions
                .orEmpty()
                .filter { it.getDirectives("namespaced").isNotEmpty() }
                .map { it.type }

            return mapFrom(
                services.flatMap forService@{ service ->
                    // Definition registry is the service's AST contributions to the overall schema
                    val operationTypes = service.definitionRegistry.getOperationTypes(operationKind)

                    val definitions = operationTypes.flatMap { it.fieldDefinitions }
                        .map { it.type }
                        .filterIsInstance(TypeName::class.java)
                        .filter { namespacedTypes.any { namespacedType -> namespacedType is GraphQLNamedSchemaElement && namespacedType.name == it.name } }

                    namespacedTypes.filterIsInstance(GraphQLNamedSchemaElement::class.java)
                        .filter { definitions.any { def -> def.name == it.name } }

                    service.definitionRegistry
                        .definitions
                        .filterIsInstance(ObjectTypeDefinition::class.java)
                        .filter { typeDef -> namespacedTypes.any { (it as GraphQLNamedSchemaElement).name == typeDef.name } }
                        .flatMap { it.fieldDefinitions }
                        .map {
                            it.name to NadelFieldInfo(
                                service,
                                operationKind,
                                it
                            )
                        } + operationTypes.flatMap { operationType ->
                        operationType.fieldDefinitions.flatMap { fieldDef ->
                            when {
                                fieldDef.hasDirective("namespaced") -> listOf()
                                else -> listOf(fieldDef.name to NadelFieldInfo(service, operationKind, fieldDef))
                            }
                        }
                    }
                }
            )
        }
    }
}
