package graphql.nadel.hooks;

import graphql.GraphQLError;
import graphql.nadel.Service;

/**
 * Represents either a {@link Service} or an error that was generated when trying to resolve the service.
 */
public class ServiceOrError {
    private final Service service;
    private final GraphQLError error;


    public ServiceOrError(Service service, GraphQLError error) {
        this.service = service;
        this.error = error;
    }

    public Service getService() {
        return service;
    }

    public GraphQLError getError() {
        return error;
    }
}
