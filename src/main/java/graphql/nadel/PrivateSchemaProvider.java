package graphql.nadel;

import com.atlassian.braid.SchemaNamespace;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Optional;

public interface PrivateSchemaProvider {
    PrivateSchemaProvider DEFAULT = namespace -> Optional.empty();

    Optional<TypeDefinitionRegistry> schemaFor(SchemaNamespace namespace);
}
