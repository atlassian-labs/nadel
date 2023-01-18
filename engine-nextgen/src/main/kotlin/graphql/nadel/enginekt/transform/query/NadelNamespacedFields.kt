package graphql.nadel.enginekt.transform.query

import graphql.nadel.schema.NadelDirectives
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema

object NadelNamespacedFields {
    fun isNamespacedField(overallField: ExecutableNormalizedField, schema: GraphQLSchema): Boolean {
        return isNamespacedField(
            overallField.getOneFieldDefinition(schema),
        )
    }

    fun isNamespacedField(field: GraphQLFieldDefinition): Boolean {
        return field.getDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name) != null
    }
}
