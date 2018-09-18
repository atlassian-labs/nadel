package graphql.nadel;

import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.document.DocumentMapperFactory;
import com.atlassian.braid.document.DocumentMappers;
import com.atlassian.braid.document.TypeMapper;
import com.atlassian.braid.source.GraphQLRemoteSchemaSource;
import graphql.Internal;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Schema source factory that uses remote graphql endpoint.
 *
 * @param <C> type of passed graphql context.
 */
@Internal
class GraphQLRemoteSchemaSourceFactory<C> implements SchemaSourceFactory {
    private final GraphQLRemoteRetrieverFactory<C> retrieverFactory;

    GraphQLRemoteSchemaSourceFactory(GraphQLRemoteRetrieverFactory<C> retrieverFactory) {
        this.retrieverFactory = retrieverFactory;
    }

    @Override
    public SchemaSource createSchemaSource(ServiceDefinition definition, SchemaNamespace namespace,
                                           TypeDefinitionRegistry typeDefinitionRegistry, List<Link> links,
                                           List<TypeMapper> mappers) {
        DocumentMapperFactory factory = DocumentMappers.identity();
        for (TypeMapper mapper : mappers){
            factory = factory.mapType(mapper);
        }
        return new GraphQLRemoteSchemaSource<>(namespace,
                typeDefinitionRegistry,
                typeDefinitionRegistry,
                retrieverFactory.createRemoteRetriever(definition),
                links,
                emptyList(),
                factory,
                emptyList(),
                emptyList(),
                emptyList());
    }
}