package graphql.nadel.hooks

import graphql.nadel.Service
import graphql.normalized.ExecutableNormalizedField
import java.util.concurrent.CompletableFuture

/**
 * These hooks allow you to change the way service execution happens
 */
interface ServiceExecutionHooks {
    /**
     * Called per top level field for a service.  This allows you to create a "context" object that will be passed into further calls.
     *
     * @param params the parameters to this call
     * @return an async context object of your choosing
     */
    fun createServiceContext(params: CreateServiceContextParams): CompletableFuture<Any?> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Called to resolve the service that should be used to fetch data for a field that uses dynamic service resolution.
     *
     *
     * There are 2 versions of this method. One passing an [ExecutionStepInfo], which is used by the CurrentGen
     * engine, and another passing [ExecutableNormalizedField], used by the NextGen engine. During the transition
     * between Current and NextGen, implementations of [ServiceExecutionHooks] will have to implement both
     * versions of this method.
     *
     * @param services                  a collection of all services registered on Nadel
     * @param executableNormalizedField object containing data about the field being executed
     * @return the Service that should be used to fetch data for that field or an error that was raised when trying to resolve the service.
     */
    fun resolveServiceForField(
        services: List<Service>,
        executableNormalizedField: ExecutableNormalizedField,
    ): ServiceOrError? {
        return null
    }
}
