package graphql.nadel;

import graphql.PublicSpi;
import graphql.schema.GraphQLSchema;

@PublicSpi
public interface ServiceDataFactory {

    ServiceExecution getDelegatedExecution(String serviceName);

    GraphQLSchema getUnderlyingSchema(String serviceName);
}
