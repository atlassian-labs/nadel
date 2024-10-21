package graphql.nadel.validation.util

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
}
