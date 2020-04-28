package graphql.nadel.engine;

import graphql.Internal;
import graphql.language.Value;
import graphql.nadel.Service;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.util.TraverserContext;

import java.util.Map;

@Internal
public class HooksVisitArgumentValueEnvironmentImpl implements HooksVisitArgumentValueEnvironment {

    private final GraphQLInputValueDefinition underlyingInputValueDefinition;
    private final GraphQLInputValueDefinition overallInputValueDefinition;
    private final TraverserContext traverserContext;
    private final Value value;
    private final Map<String, Object> variables;
    private final Service service;
    private final Object serviceContext;
    private final GraphQLArgument underlyingGraphQLArgument;
    private final GraphQLArgument overallGraphQLArgument;

    public HooksVisitArgumentValueEnvironmentImpl(GraphQLInputValueDefinition underlyingInputValueDefinition,
                                                  GraphQLInputValueDefinition overallInputValueDefinition,
                                                  GraphQLArgument underlyingGraphQLArgument,
                                                  GraphQLArgument overallGraphQLArgument,
                                                  TraverserContext traverserContext,
                                                  Value value,
                                                  Map<String, Object> variables,
                                                  Service service,
                                                  Object serviceContext) {
        this.underlyingInputValueDefinition = underlyingInputValueDefinition;
        this.overallInputValueDefinition = overallInputValueDefinition;
        this.underlyingGraphQLArgument = underlyingGraphQLArgument;
        this.overallGraphQLArgument = overallGraphQLArgument;
        this.traverserContext = traverserContext;
        this.value = value;
        this.variables = variables;
        this.service = service;
        this.serviceContext = serviceContext;
    }

    @Override
    public GraphQLInputValueDefinition getUnderlyingInputValueDefinition() {
        return underlyingInputValueDefinition;
    }

    @Override
    public GraphQLInputValueDefinition getOverallInputValueDefinition() {
        return overallInputValueDefinition;
    }

    @Override
    public GraphQLArgument getUnderlyingGraphQLArgument() {
        return overallGraphQLArgument;
    }

    @Override
    public GraphQLArgument getOverallGraphQLArgument() {
        return overallGraphQLArgument;
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
