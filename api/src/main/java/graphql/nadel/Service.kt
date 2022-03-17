package graphql.nadel;

import graphql.PublicApi;
import graphql.schema.GraphQLSchema;

@PublicApi
public class Service {

    private final String name;
    private final GraphQLSchema underlyingSchema;
    // this is not enough in the future as we need to allow for dynamic delegationExecution
    private final ServiceExecution serviceExecution;
    private final NadelDefinitionRegistry nadelDefinitionRegistry;

    public Service(String name,
                   GraphQLSchema underlyingSchema,
                   ServiceExecution serviceExecution,
                   NadelDefinitionRegistry nadelDefinitionRegistry) {
        this.name = name;
        this.underlyingSchema = underlyingSchema;
        this.serviceExecution = serviceExecution;
        this.nadelDefinitionRegistry = nadelDefinitionRegistry;
    }

    public String getName() {
        return name;
    }

    /**
     * These are the types as they are defined in the underlying service's schema.
     * <p>
     * There are no renames, hydrations etc.
     */
    public GraphQLSchema getUnderlyingSchema() {
        return underlyingSchema;
    }

    public ServiceExecution getServiceExecution() {
        return serviceExecution;
    }

    /**
     * These are the GraphQL definitions that a service contributes to the OVERALL schema.
     */
    public NadelDefinitionRegistry getDefinitionRegistry() {
        return nadelDefinitionRegistry;
    }
}
