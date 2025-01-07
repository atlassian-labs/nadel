package graphql.nadel.validation

import graphql.nadel.definition.NadelDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLSchema

abstract class NadelSchemaValidationHook {
    open fun parseDefinitions(
        engineSchema: GraphQLSchema,
        parent: GraphQLFieldsContainer,
        field: GraphQLFieldDefinition,
    ): List<NadelDefinition> {
        return emptyList()
    }
}
