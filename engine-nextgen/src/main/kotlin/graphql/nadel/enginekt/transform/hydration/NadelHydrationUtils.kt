package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

internal object NadelHydrationUtils {
    fun getSourceFieldDefinition(
        instruction: NadelHydrationFieldInstruction,
    ): GraphQLFieldDefinition {
        return getSourceFieldDefinition(
            service = instruction.sourceService,
            pathToSourceField = instruction.pathToSourceField,
        )
    }

    private fun getSourceFieldDefinition(
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
