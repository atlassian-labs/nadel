package graphql.nadel;

import graphql.nadel.dsl.ServiceDefinition;

public interface GraphqlCallerFactory {

    GraphqlCaller createGraphqlCaller(ServiceDefinition serviceDefinition);

}
