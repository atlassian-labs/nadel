package graphql.nadel;

import graphql.PublicApi;
import graphql.schema.GraphQLSchema;

@PublicApi
public class Service {

    private final String name;
    private final GraphQLSchema privateSchema;
    // this is not enough in the future as we need to allow for dynamic delegationExecution
    private DelegatedExecution delegatedExecution;

    public Service(String name, GraphQLSchema privateSchema, DelegatedExecution delegatedExecution) {
        this.name = name;
        this.privateSchema = privateSchema;
        this.delegatedExecution = delegatedExecution;
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
}
