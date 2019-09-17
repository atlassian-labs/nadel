package graphql.nadel.engine;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.util.TraverserContext;

public class HooksVisitArgumentValueEnvironmentImpl implements HooksVisitArgumentValueEnvironment {

    private final GraphQLInputValueDefinition inputValueDefinition;
    private final TraverserContext context;

    public HooksVisitArgumentValueEnvironmentImpl(GraphQLInputValueDefinition inputValueDefinition, TraverserContext context) {
        this.inputValueDefinition = inputValueDefinition;
        this.context = context;
    }

    @Override
    public GraphQLInputValueDefinition getInputValueDefinition() {
        return inputValueDefinition;
    }

    @Override
    public GraphQLArgument getGraphQLArgument() {
        return null;
    }

    @Override
    public TraverserContext getContext() {
        return context;
    }
}
