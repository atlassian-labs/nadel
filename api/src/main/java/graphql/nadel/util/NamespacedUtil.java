package graphql.nadel.util;

import graphql.language.ObjectTypeDefinition;
import graphql.language.ObjectTypeExtensionDefinition;
import graphql.nadel.Service;
import graphql.schema.GraphQLObjectType;

public final class NamespacedUtil {
    private NamespacedUtil() {
    }

    public static boolean serviceOwnsNamespacedField(GraphQLObjectType namespacedObjectType, Service service) {
        return serviceOwnsNamespacedField(namespacedObjectType.getName(), service);
    }

    public static boolean serviceOwnsNamespacedField(String namespacedObjectTypeName, Service service) {
        return service.getDefinitionRegistry()
            .getDefinitions(ObjectTypeDefinition.class)
            .stream()
            // the type can't be an extension in the owning service
            .filter(objectTypeDef -> !(objectTypeDef instanceof ObjectTypeExtensionDefinition))
            .anyMatch(objectTypeDef -> objectTypeDef.getName().equals(namespacedObjectTypeName));
    }
}
