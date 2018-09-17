package graphql.nadel;

import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Map;

/**
 * Factory that creates {@link TypeDefinitionRegistry} that will be added to a stitched schema.
 */
public interface TypeDefinitionRegistryFactory {
    TypeDefinitionRegistryFactory DEFAULT = (Map<String, TypeDefinitionRegistry> serviceTypeDefinitions) ->
            new TypeDefinitionRegistry();

    /**
     * Creates {@link TypeDefinitionRegistry}.
     *
     * @param serviceTypeDefinitions map containing  {@link TypeDefinitionRegistry} (as a value) for each service name
     *                               (key) defined in Nadel DSL. May be used to add new types based on directives etc.
     *
     * @return type definition that will be added to a stitched schema, cannot be null.
     */
    TypeDefinitionRegistry create(Map<String, TypeDefinitionRegistry> serviceTypeDefinitions);
}
