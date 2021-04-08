package graphql.nadel.schema;

import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.PublicApi;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.List;

/**
 * This exception wraps a {@link graphql.schema.idl.errors.SchemaProblem} and associates the specific
 * problems with a service name
 */
@PublicApi
public class ServiceSchemaProblem extends GraphQLException {

    private final String serviceName;
    private final SchemaProblem cause;

    public ServiceSchemaProblem(String message, String serviceName, SchemaProblem cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.cause = cause;
    }

    public String getServiceName() {
        return serviceName;
    }

    public List<GraphQLError> getErrors() {
        return cause.getErrors();
    }

    @Override
    public String toString() {
        return "ServiceSchemaProblem{" +
                "service=" + serviceName +
                ", errors=" + getErrors() +
                '}';
    }
}
