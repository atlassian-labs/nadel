package graphql.nadel;

import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Map;

import static graphql.nadel.TypeDefinitionsWithRuntimeWiring.newTypeDefinitionWithRuntimeWiring;

/**
 * Factory that provides {@link TypeDefinitionsWithRuntimeWiring} to be added to a stitched schema.
 */
public interface TypeDefinitionRegistryFactory {
    TypeDefinitionRegistryFactory DEFAULT = (Map<String, TypeDefinitionRegistry> serviceTypeDefinitions) ->
            newTypeDefinitionWithRuntimeWiring().build();

    /**
     * Creates {@link TypeDefinitionsWithRuntimeWiring} that might be based on existing types defined in DSL.
     *
     * @param serviceTypeDefinitions map containing  {@link TypeDefinitionRegistry} (as a value) for each service name
     *                               (key) defined in Nadel DSL. May be used to add new types based on directives etc.
     *
     * @return type definitions and runtime wirings that will be added to a stitched schema, cannot be null.
     */
    TypeDefinitionsWithRuntimeWiring create(Map<String, TypeDefinitionRegistry> serviceTypeDefinitions);
}
