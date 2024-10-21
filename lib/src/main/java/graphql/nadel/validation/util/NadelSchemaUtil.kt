package graphql.nadel.validation.util

import graphql.language.FieldDefinition
import graphql.language.OperationDefinition
import graphql.nadel.Service
import graphql.nadel.definition.renamed.getRenamedOrNull
import graphql.nadel.schema.NadelDirectives
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType

internal object NadelSchemaUtil {
    fun getUnderlyingType(overallType: GraphQLNamedType, service: Service): GraphQLNamedType? {
        return service.underlyingSchema.getType(getUnderlyingName(overallType)) as GraphQLNamedType?
    }

    fun getUnderlyingName(type: GraphQLNamedType): String {
        return type.getRenamedOrNull()?.from ?: type.name
    }

    @Deprecated(message = "To be replaced with directive wrapper class and extensions")
    fun hasPartition(field: GraphQLFieldDefinition): Boolean {
        return hasPartition(field.definition!!)
    }

    @Deprecated(message = "To be replaced with directive wrapper class and extensions")
    fun hasPartition(def: FieldDefinition): Boolean {
        return def.hasDirective(NadelDirectives.partitionDirectiveDefinition.name)
    }

    fun isOperation(type: GraphQLNamedType): Boolean {
        return type.name.equals(OperationDefinition.Operation.QUERY.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.MUTATION.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.SUBSCRIPTION.toString(), ignoreCase = true)
    }
}
