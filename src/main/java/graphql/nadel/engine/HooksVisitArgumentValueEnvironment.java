package graphql.nadel.engine;

import graphql.language.Node;
import graphql.language.Value;
import graphql.nadel.Service;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.util.TraverserContext;

import java.util.Map;

public interface HooksVisitArgumentValueEnvironment {

    Service getService();

    Object getServiceContext();

    GraphQLInputValueDefinition getUnderlyingInputValueDefinition();

    /**
     * @return this can be null if there is no corresponding overall input value definition
     */
    GraphQLInputValueDefinition getOverallInputValueDefinition();

    GraphQLArgument getUnderlyingGraphQLArgument();

    /**
     * @return this can be null if there is no corresponding overall argument
     */
    GraphQLArgument getOverallGraphQLArgument();

    TraverserContext<Node> getTraverserContext();

    Value getValue();

    Map<String, Object> getVariables();

}
