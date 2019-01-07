package graphql.nadel;

import graphql.PublicApi;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.TypeDefinitionRegistry;

@PublicApi
public class Service {

    private final String name;
    private final GraphQLSchema privateSchema;
    // this is not enough in the future as we need to allow for dynamic delegationExecution
    private final DelegatedExecution delegatedExecution;
    private final ServiceDefinition serviceDefinition;
    private TypeDefinitionRegistry typeDefinitionRegistry;


    public Service(String name,
                   GraphQLSchema privateSchema,
                   DelegatedExecution delegatedExecution,
                   ServiceDefinition serviceDefinition,
                   TypeDefinitionRegistry typeDefinitionRegistry) {
        this.name = name;
        this.privateSchema = privateSchema;
        this.delegatedExecution = delegatedExecution;
        this.serviceDefinition = serviceDefinition;
        this.typeDefinitionRegistry = typeDefinitionRegistry;
    }

    public String getName() {
        return name;
    }

    public GraphQLSchema getPrivateSchema() {
        return privateSchema;
    }

    public DelegatedExecution getDelegatedExecution() {
        return delegatedExecution;
    }

    public ServiceDefinition getServiceDefinition() {
        return serviceDefinition;
    }

    public TypeDefinitionRegistry getTypeDefinitionRegistry() {
        return typeDefinitionRegistry;
    }
}
