package graphql.nadel;

import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;

public enum Operation {
    QUERY("query", "Query", OperationDefinition.Operation.QUERY),
    MUTATION("mutation", "Mutation", OperationDefinition.Operation.MUTATION),
    SUBSCRIPTION("subscription", "Subscription", OperationDefinition.Operation.SUBSCRIPTION);

    private final String name;
    private final String displayName;
    private final OperationDefinition.Operation astOperation;

    private Operation(String name, String displayName, OperationDefinition.Operation astOperation) {
        this.displayName = displayName;
        this.name = name;
        this.astOperation = astOperation;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public OperationDefinition.Operation getAstOperation() {
        return astOperation;
    }

    public static Operation fromAst(OperationDefinition.Operation operation) {
        if (operation == null) {
            return QUERY;
        }
        switch (operation) {
            case QUERY:
                return QUERY;
            case MUTATION:
                return MUTATION;
            case SUBSCRIPTION:
                return SUBSCRIPTION;
            default:
                return assertShouldNeverHappen();
        }
    }

    public GraphQLObjectType getRootType(GraphQLSchema schema) {
        switch (this) {
            case QUERY:
                return assertNotNull(schema.getQueryType());
            case MUTATION:
                return assertNotNull(schema.getMutationType());
            default:
                return assertShouldNeverHappen();
        }
    }

}