package graphql.nadel.util

import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

object NamespacedUtil {
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

    fun isNamespacedLike(definition: GraphQLFieldDefinition): Boolean {
        return isNamespacedField(definition) ||
            (definition.arguments.isEmpty() && definition.type.unwrapNonNull() is GraphQLObjectType)
    }
}
