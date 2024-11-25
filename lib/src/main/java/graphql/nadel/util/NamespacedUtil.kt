package graphql.nadel.util

import graphql.language.ObjectTypeDefinition
import graphql.nadel.Service
import graphql.nadel.engine.util.isExtensionDef
import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema

object NamespacedUtil {
    fun serviceOwnsNamespacedField(namespacedObjectTypeName: String, service: Service): Boolean {
        return service
            .definitionRegistry
            .getDefinitions(namespacedObjectTypeName)
            .asSequence()
            .filterIsInstance<ObjectTypeDefinition>()
            .filterNot { it.isExtensionDef }
            .any { it.name == namespacedObjectTypeName }
    }

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
