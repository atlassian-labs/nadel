package graphql.nadel

import graphql.PublicSpi
import graphql.schema.idl.TypeDefinitionRegistry

@PublicSpi
interface ServiceExecutionFactory {
    /**
     * Called to to get a function that can be called to get data for the named service
     *
     * @param serviceName the name of the service
     *
     * @return a function that can be called to get data from that service
     */
    fun getServiceExecution(serviceName: String): ServiceExecution

    /**
     * todo: why does this belong here and not in the Nadel constructor?
     *
     * Called to return underlying type definitions for a schema
     *
     * @param serviceName the name of the service
     *
     * @return a [graphql.schema.idl.TypeDefinitionRegistry] of all the underlying schema types
     */
    fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry
}
