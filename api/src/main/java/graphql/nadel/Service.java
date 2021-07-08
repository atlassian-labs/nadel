package graphql.nadel;

import graphql.PublicApi;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.GraphQLSchema;

@PublicApi
public class Service {

    private final String name;
    private final GraphQLSchema underlyingSchema;
    // this is not enough in the future as we need to allow for dynamic delegationExecution
    private final ServiceExecution serviceExecution;
    private final ServiceDefinition serviceDefinition;
    private final DefinitionRegistry definitionRegistry;

    public Service(String name,
                   GraphQLSchema underlyingSchema,
                   ServiceExecution serviceExecution,
                   ServiceDefinition serviceDefinition,
                   DefinitionRegistry definitionRegistry) {
        this.name = name;
        this.underlyingSchema = underlyingSchema;
        this.serviceExecution = serviceExecution;
        this.serviceDefinition = serviceDefinition;
        this.definitionRegistry = definitionRegistry;
    }

    public String getName() {
        return name;
    }

    public GraphQLSchema getUnderlyingSchema() {
        return underlyingSchema;
    }

    public ServiceExecution getServiceExecution() {
        return serviceExecution;
    }

    public ServiceDefinition getServiceDefinition() {
        return serviceDefinition;
    }

    public DefinitionRegistry getDefinitionRegistry() {
        return definitionRegistry;
    }
}
