package graphql.nadel;

import com.atlassian.braid.source.GraphQLRemoteRetriever;
import graphql.nadel.dsl.ServiceDefinition;

public interface GraphQLRemoteRetrieverFactory {

    GraphQLRemoteRetriever createRemoteRetriever(ServiceDefinition serviceDefinition);
}
