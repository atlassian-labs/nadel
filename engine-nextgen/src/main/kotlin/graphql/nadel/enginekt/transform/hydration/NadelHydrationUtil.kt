package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

internal object NadelHydrationUtil {
    fun getActorField(
        instruction: NadelGenericHydrationInstruction,
    ): GraphQLFieldDefinition {
        return getActorField(
            service = instruction.actorService,
            queryPathToActorField = instruction.queryPathToActorField,
        )
    }

    private fun getActorField(
        service: Service,
        queryPathToActorField: QueryPath,
    ): GraphQLFieldDefinition {
        val parentType = queryPathToActorField
            .segments
            .asSequence()
            .take(queryPathToActorField.segments.size - 1) // All but last element
            .fold(service.underlyingSchema.queryType) { prevType, fieldName ->
                prevType.getField(fieldName)!!.type as GraphQLObjectType
            }

        return parentType.getField(queryPathToActorField.last())
    }
}
