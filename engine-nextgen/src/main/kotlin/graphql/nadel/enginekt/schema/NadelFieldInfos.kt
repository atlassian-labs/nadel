package graphql.nadel.enginekt.schema

import graphql.nadel.OperationKind
import graphql.nadel.Service
import graphql.nadel.enginekt.util.getOperationTypes
import graphql.nadel.enginekt.util.mapFrom

internal data class NadelFieldInfos(
    private val queryTopLevelFields: Map<String, NadelFieldInfo>,
    private val mutationTopLevelFields: Map<String, NadelFieldInfo>,
    private val subscriptionTopLevelFields: Map<String, NadelFieldInfo>,
) {
    fun getFieldInfo(operationKind: OperationKind, topLevelFieldName: String): NadelFieldInfo? {
        return when (operationKind) {
            OperationKind.QUERY -> queryTopLevelFields[topLevelFieldName]
            OperationKind.MUTATION -> mutationTopLevelFields[topLevelFieldName]
            OperationKind.SUBSCRIPTION -> subscriptionTopLevelFields[topLevelFieldName]
        }
    }

    companion object {
        fun create(services: List<Service>): NadelFieldInfos {
            return NadelFieldInfos(
                getInfosFromServices(services, OperationKind.QUERY),
                getInfosFromServices(services, OperationKind.MUTATION),
                getInfosFromServices(services, OperationKind.SUBSCRIPTION),
            )
        }

        private fun getInfosFromServices(
            services: List<Service>,
            operationKind: OperationKind,
        ): Map<String, NadelFieldInfo> {
            return mapFrom(
                services.flatMap forService@{ service ->
                    // Definition registry is the service's AST contributions to the overall schema
                    val operationTypes = service.definitionRegistry.getOperationTypes(operationKind)
                    operationTypes.flatMap { operationType ->
                        operationType.fieldDefinitions.map { fieldDef ->
                            fieldDef.name to NadelFieldInfo(service, operationKind, fieldDef)
                        }
                    }
                }
            )
        }
    }
}
