package graphql.nadel.engine;

import graphql.language.Value;
import graphql.nadel.Service;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.util.TraverserContext;

import java.util.Map;

public class HooksVisitArgumentValueEnvironmentImpl implements HooksVisitArgumentValueEnvironment {

    private final GraphQLInputValueDefinition inputValueDefinition;
    private final TraverserContext traverserContext;
    private final Value value;
    private final Map<String, Object> variables;
    private final Service service;
    private final Object serviceContext;
    private final GraphQLArgument graphQLArgument;

    public HooksVisitArgumentValueEnvironmentImpl(GraphQLInputValueDefinition inputValueDefinition,
                                                  GraphQLArgument graphQLArgument,
                                                  TraverserContext traverserContext,
                                                  Value value,
                                                  Map<String, Object> variables,
                                                  Service service,
                                                  Object serviceContext) {
        this.inputValueDefinition = inputValueDefinition;
        this.graphQLArgument = graphQLArgument;
        this.traverserContext = traverserContext;
        this.value = value;
        this.variables = variables;
        this.service = service;
        this.serviceContext = serviceContext;
    }

    @Override
    public GraphQLInputValueDefinition getInputValueDefinition() {
        return inputValueDefinition;
    }

    @Override
    public GraphQLArgument getGraphQLArgument() {
        return graphQLArgument;
    }


    @Override
    public Value getValue() {
        return value;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public Object getServiceContext() {
        return serviceContext;
    }

    @Override
    public TraverserContext getTraverserContext() {
        return traverserContext;
    }
}
