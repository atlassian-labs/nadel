package graphql.nadel.util

import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.nadel.Service
import graphql.nadel.engine.util.operationTypes
import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

object NamespacedUtil {
    fun serviceOwnsNamespacedField(namespacedObjectType: GraphQLObjectType, service: Service): Boolean {
        return serviceOwnsNamespacedField(namespacedObjectType.name, service)
    }

    fun serviceOwnsNamespacedField(namespacedObjectTypeName: String, service: Service): Boolean {
        return service
            .definitionRegistry
            .getDefinitionsOfType<ObjectTypeDefinition>()
            .stream() // the type can't be an extension in the owning service
            .filter { it !is ObjectTypeExtensionDefinition }
            .anyMatch { it.name == namespacedObjectTypeName }
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

    /**
     * Even if this returns "true", nothing prevents this type from being used in a "non-namespace" field
     */
    fun isNamespaceType(type: GraphQLNamedType, schema: GraphQLSchema): Boolean {
        val namespaceFieldsWithThisType = schema.operationTypes
            .asSequence()
            .flatMap { it.fieldDefinitions }
            .filter { NamespacedUtil.isNamespacedField(it) }
            .map { it.type }
            .filterIsInstance<GraphQLNamedOutputType>()
            .filter {it.name == type.name}
            .toList()

        return namespaceFieldsWithThisType.size == 1
    }
}
