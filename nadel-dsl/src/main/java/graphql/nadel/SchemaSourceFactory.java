package graphql.nadel;


import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.List;

/**
 * Interface for creating {@link SchemaSource}s used by nadel.
 */
public interface SchemaSourceFactory {
    SchemaSource createSchemaSource(ServiceDefinition definition, SchemaNamespace namespace, TypeDefinitionRegistry typeDefinitionRegistry,
                                    List<Link> links);
}
