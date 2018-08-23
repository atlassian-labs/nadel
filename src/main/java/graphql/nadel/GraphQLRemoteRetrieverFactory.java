package graphql.nadel;

import com.atlassian.braid.source.GraphQLRemoteRetriever;
import graphql.nadel.dsl.ServiceDefinition;

/**
 * Creates GraphQL remote retriever for a given service definition.
 *
 * @param <C> type of graphql exection context.
 */
public interface GraphQLRemoteRetrieverFactory<C> {
    GraphQLRemoteRetriever<C> createRemoteRetriever(ServiceDefinition serviceDefinition);
}
