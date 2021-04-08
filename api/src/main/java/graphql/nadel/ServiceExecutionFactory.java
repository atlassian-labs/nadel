package graphql.nadel;

import graphql.PublicSpi;
import graphql.schema.idl.TypeDefinitionRegistry;

@PublicSpi
public interface ServiceExecutionFactory {

    /**
     * Called to to get a function that can be called to get data for the named service
     *
     * @param serviceName the name of the service
     *
     * @return a function that can be called to get data from that service
     */
    ServiceExecution getServiceExecution(String serviceName);

    /**
     * Called to return underlying type definitions for a schema
     *
     * @param serviceName the name of the service
     *
     * @return a {@link graphql.schema.idl.TypeDefinitionRegistry} of all the underlying schema types
     */
    TypeDefinitionRegistry getUnderlyingTypeDefinitions(String serviceName);
}
