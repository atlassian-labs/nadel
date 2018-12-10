package graphql.nadel;

import com.atlassian.braid.SchemaNamespace;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Optional;

/**
 * Provides optional private schema.
 */
public interface PrivateSchemaProvider {
    PrivateSchemaProvider DEFAULT = namespace -> Optional.empty();

    /**
     *
     * @param namespace of the service.
     * @return private schema for the namespace or {@link Optional#empty()}.
     */
    Optional<TypeDefinitionRegistry> schemaFor(SchemaNamespace namespace);
}
