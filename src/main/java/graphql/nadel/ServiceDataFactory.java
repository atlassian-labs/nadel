package graphql.nadel;

import graphql.PublicSpi;
import graphql.schema.GraphQLSchema;

@PublicSpi
public interface ServiceDataFactory {

    DelegatedExecution getDelegatedExecution(String serviceName);

    GraphQLSchema getPrivateSchema(String serviceName);
}
