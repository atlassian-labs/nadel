package graphql.nadel.enginekt.transform.query

import graphql.nadel.schema.NadelDirectives
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLSchema

object NadelNamespacedFields {
    fun isNamespacedField(overallField: ExecutableNormalizedField, schema: GraphQLSchema): Boolean {
        return overallField
            .getFieldDefinitions(schema)
            .any {
                it.hasAppliedDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name)
            }
    }
}
