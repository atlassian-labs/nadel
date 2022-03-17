package graphql.nadel.hooks;

import graphql.PublicSpi;
import graphql.execution.ExecutionStepInfo;
import graphql.nadel.Service;
import graphql.normalized.ExecutableNormalizedField;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * These hooks allow you to change the way service execution happens
 */
@PublicSpi
public interface ServiceExecutionHooks {

    /**
     * Called per top level field for a service.  This allows you to create a "context" object that will be passed into further calls.
     *
     * @param params the parameters to this call
     * @return an async context object of your choosing
     */
    default CompletableFuture<Object> createServiceContext(CreateServiceContextParams params) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Called to resolve the service that should be used to fetch data for a field that uses dynamic service resolution.
     * <p>
     * There are 2 versions of this method. One passing an {@link ExecutionStepInfo}, which is used by the CurrentGen
     * engine, and another passing {@link ExecutableNormalizedField}, used by the NextGen engine. During the transition
     * between Current and NextGen, implementations of {@link ServiceExecutionHooks} will have to implement both
     * versions of this method.
     *
     * @param services                  a collection of all services registered on Nadel
     * @param executableNormalizedField object containing data about the field being executed
     * @return the Service that should be used to fetch data for that field or an error that was raised when trying to resolve the service.
     */
    @Nullable
    default ServiceOrError resolveServiceForField(Collection<Service> services, ExecutableNormalizedField executableNormalizedField) {
        return null;
    }
}
