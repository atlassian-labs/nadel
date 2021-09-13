package graphql.nadel.hooks;

import graphql.GraphQLError;
import graphql.PublicSpi;
import graphql.execution.ExecutionStepInfo;
import graphql.nadel.Service;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.normalized.ExecutableNormalizedField;
import graphql.schema.GraphQLSchema;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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

    @Nullable
    default NewVariableValue visitArgumentValueInQuery(HooksVisitArgumentValueEnvironment env) {
        return null;
    }

    /**
     * Called to determine whether a field is forbidden which means it should be omitted from the query to the underlying service.
     * When a field is forbidden, the field is set to null and a GraphQL error is inserted into the overall response.
     *
     * @param normalizedField     the field in question
     * @param hydrationArguments  arguments supplied to the top level field of the hydration
     * @param variables           query variables
     * @param graphQLSchema       overall graphQL schema
     * @param userSuppliedContext the context supplied to Nadel in {@link graphql.nadel.NadelExecutionInput}
     * @return an error if the field should be omitted, empty optional otherwise
     */
    default CompletableFuture<Optional<GraphQLError>> isFieldForbidden(
            NormalizedQueryField normalizedField,
            HydrationArguments hydrationArguments,
            Map<String, Object> variables,
            GraphQLSchema graphQLSchema,
            Object userSuppliedContext
    ) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /***
     * Called to resolve the service that should be used to fetch data for a field that uses dynamic service resolution.
     *
     * There are 2 versions of this method. One passing an {@link ExecutionStepInfo}, which is used by the CurrentGen
     * engine, and another passing {@link ExecutableNormalizedField}, used by the NextGen engine. During the transition
     * between Current and NextGen, implementations of {@link ServiceExecutionHooks} will have to implement both
     * versions of this method.
     *
     * @param services a collection of all services registered on Nadel
     * @param executionStepInfo object containing data about the field being executed
     * @return the Service that should be used to fetch data for that field or an error that was raised when trying to resolve the service.
     * @see ServiceExecutionHooks#resolveServiceForField(Collection, ExecutableNormalizedField) for the Next Gen implementation
     */
    @Nullable
    default ServiceOrError resolveServiceForField(Collection<Service> services, ExecutionStepInfo executionStepInfo) {
        return null;
    }

    /**
     * Called to resolve the service that should be used to fetch data for a field that uses dynamic service resolution.
     *
     * There are 2 versions of this method. One passing an {@link ExecutionStepInfo}, which is used by the CurrentGen
     * engine, and another passing {@link ExecutableNormalizedField}, used by the NextGen engine. During the transition
     * between Current and NextGen, implementations of {@link ServiceExecutionHooks} will have to implement both
     * versions of this method.
     *
     * @param services a collection of all services registered on Nadel
     * @param executableNormalizedField object containing data about the field being executed
     * @return the Service that should be used to fetch data for that field or an error that was raised when trying to resolve the service.
     * @see ServiceExecutionHooks#resolveServiceForField(Collection, ExecutionStepInfo) for the Current Gen implementation.
     */
    @Nullable
    default ServiceOrError resolveServiceForField(Collection<Service> services, ExecutableNormalizedField executableNormalizedField) {
        return null;
    }
}
