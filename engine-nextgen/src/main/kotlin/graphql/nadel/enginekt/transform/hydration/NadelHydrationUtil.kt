package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

internal object NadelHydrationUtil {
    fun getSourceFieldDefinition(
        instruction: NadelGenericHydrationInstruction,
    ): GraphQLFieldDefinition {
        return getSourceFieldDefinition(
            service = instruction.actorService,
            pathToSourceField = instruction.queryPathToActorField,
        )
    }

    private fun getSourceFieldDefinition(
        service: Service,
        pathToSourceField: QueryPath,
    ): GraphQLFieldDefinition {
        val parentType = pathToSourceField
            .segments
            .asSequence()
            .take(pathToSourceField.segments.size - 1) // All but last element
            .fold(service.underlyingSchema.queryType) { prevType, fieldName ->
                prevType.getField(fieldName)!!.type as GraphQLObjectType
            }

        return parentType.getField(pathToSourceField.last())
    }
}
