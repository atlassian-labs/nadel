package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

internal object NadelHydrationUtils {
    fun getSourceField(
        service: Service,
        pathToSourceField: List<String>,
    ): GraphQLFieldDefinition {
        val parentType = pathToSourceField
            .asSequence()
            .take(pathToSourceField.size - 1) // All but last element
            .fold(service.underlyingSchema.queryType) { prevType, fieldName ->
                prevType.fields.find { it.name == fieldName }!!.type as GraphQLObjectType
            }

        return parentType.getField(pathToSourceField.last())
    }
}
