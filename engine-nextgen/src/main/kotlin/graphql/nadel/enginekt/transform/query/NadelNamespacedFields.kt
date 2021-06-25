package graphql.nadel.enginekt.transform.query

import graphql.nadel.schema.NadelDirectives
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

object NadelNamespacedFields {
    fun isNamespacedField(overallField: NormalizedField, schema: GraphQLSchema): Boolean {
        return overallField.getOneFieldDefinition(schema)
            .getDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name) != null
    }
}
