package graphql.nadel.util

import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.nadel.Service
import graphql.schema.GraphQLObjectType

internal object NamespacedUtil {
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
}
