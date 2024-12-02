package graphql.nadel.validation.util

import graphql.language.OperationDefinition
import graphql.nadel.Service
import graphql.nadel.definition.renamed.getRenamedOrNull
import graphql.schema.GraphQLNamedType

internal object NadelSchemaUtil {
    fun getUnderlyingType(overallType: GraphQLNamedType, service: Service): GraphQLNamedType? {
        return service.underlyingSchema.getType(getUnderlyingName(overallType)) as GraphQLNamedType?
    }

    fun getUnderlyingName(type: GraphQLNamedType): String {
        return type.getRenamedOrNull()?.from ?: type.name
    }

    fun isOperation(type: GraphQLNamedType): Boolean {
        return type.name.equals(OperationDefinition.Operation.QUERY.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.MUTATION.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.SUBSCRIPTION.toString(), ignoreCase = true)
    }
}
