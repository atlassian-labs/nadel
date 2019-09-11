package graphql.nadel.engine;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;

public class HooksVisitArgumentValueEnvironmentImpl implements HooksVisitArgumentValueEnvironment {

    private final GraphQLInputValueDefinition inputValueDefinition;

    public HooksVisitArgumentValueEnvironmentImpl(GraphQLInputValueDefinition inputValueDefinition) {
        this.inputValueDefinition = inputValueDefinition;
    }

    @Override
    public GraphQLInputValueDefinition getInputValueDefinition() {
        return inputValueDefinition;
    }

    @Override
    public GraphQLArgument getGraphQLArgument() {
        return null;
    }
}
