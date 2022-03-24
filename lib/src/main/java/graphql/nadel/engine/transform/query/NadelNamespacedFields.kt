package graphql.nadel.engine.transform.query

import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLSchema

object NadelNamespacedFields {
    fun isNamespacedField(overallField: ExecutableNormalizedField, schema: GraphQLSchema): Boolean {
        return overallField
            .getFieldDefinitions(schema)
            .any {
                it.hasAppliedDirective(namespacedDirectiveDefinition.name)
            }
    }
}
