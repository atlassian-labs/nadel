package graphql.nadel.enginekt.transform.query

import graphql.nadel.schema.NadelDirectives
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

object NadelNamespacedFields {
    fun isNamespacedField(field: NormalizedField, schema: GraphQLSchema): Boolean {
        return field.getOneFieldDefinition(schema)
            .getDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name) != null
    }
}
