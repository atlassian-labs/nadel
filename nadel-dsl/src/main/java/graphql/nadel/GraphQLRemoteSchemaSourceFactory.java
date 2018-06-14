package graphql.nadel;


import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.document.DocumentMappers;
import com.atlassian.braid.source.GraphQLRemoteSchemaSource;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.List;

public class GraphQLRemoteSchemaSourceFactory implements SchemaSourceFactory {
    private final GraphQLRemoteRetrieverFactory retrieverFactory;

    public GraphQLRemoteSchemaSourceFactory(GraphQLRemoteRetrieverFactory retrieverFactory) {
        this.retrieverFactory = retrieverFactory;
    }

    @Override
    public SchemaSource createSchemaSource(ServiceDefinition definition, SchemaNamespace namespace,
                                           TypeDefinitionRegistry typeDefinitionRegistry, List<Link> links) {
        return new GraphQLRemoteSchemaSource<>(namespace,
                typeDefinitionRegistry,
                typeDefinitionRegistry,
                retrieverFactory.createRemoteRetriever(definition),
                links,
                DocumentMappers.identity());
    }
}
