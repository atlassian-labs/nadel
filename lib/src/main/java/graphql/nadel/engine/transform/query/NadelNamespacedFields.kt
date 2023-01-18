package graphql.nadel.engine.transform.query

import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema

object NadelNamespacedFields {
    fun isNamespacedField(overallField: ExecutableNormalizedField, schema: GraphQLSchema): Boolean {
        return overallField
            .getFieldDefinitions(schema)
            .any {
                isNamespacedField(it)
            }
    }

    fun isNamespacedField(definition: GraphQLFieldDefinition): Boolean {
        return definition.hasAppliedDirective(namespacedDirectiveDefinition.name)
    }
}
