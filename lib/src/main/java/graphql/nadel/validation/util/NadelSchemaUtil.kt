package graphql.nadel.validation.util

import graphql.language.OperationDefinition
import graphql.nadel.Service
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.getUnderlyingTypeName
import graphql.schema.GraphQLNamedType

internal object NadelSchemaUtil {
    context(NadelValidationContext)
    fun getUnderlyingType(overallType: GraphQLNamedType, service: Service): GraphQLNamedType? {
        return service.underlyingSchema.getType(getUnderlyingTypeName(overallType)) as GraphQLNamedType?
    }

    fun isOperation(type: GraphQLNamedType): Boolean {
        return type.name.equals(OperationDefinition.Operation.QUERY.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.MUTATION.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.SUBSCRIPTION.toString(), ignoreCase = true)
    }
}
