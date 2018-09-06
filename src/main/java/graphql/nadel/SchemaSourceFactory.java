package graphql.nadel;


import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.List;

/**
 * Factory for schema source.
 */
public interface SchemaSourceFactory {
    SchemaSourceFactory DEFAULT = (definition, namespace, typeDefinitionRegistry, links) -> null;

    SchemaSource createSchemaSource(ServiceDefinition definition, SchemaNamespace namespace,
                                    TypeDefinitionRegistry typeDefinitionRegistry, List<Link> links);
}
