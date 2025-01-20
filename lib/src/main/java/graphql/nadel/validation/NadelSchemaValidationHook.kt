package graphql.nadel.validation

import graphql.nadel.definition.NadelInstructionDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLSchema

abstract class NadelSchemaValidationHook {
    open fun parseDefinitions(
        engineSchema: GraphQLSchema,
        parent: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): List<NadelInstructionDefinition> {
        return emptyList()
    }
}
