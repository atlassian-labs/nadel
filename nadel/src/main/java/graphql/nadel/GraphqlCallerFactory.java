package graphql.nadel;

import graphql.nadel.dsl.ServiceDefinition;

public interface GraphqlCallerFactory {

//    class GraphqlCallerEnvironment {
//
//    }

    GraphqlCaller createGraphqlCaller(ServiceDefinition serviceDefinition);

}
