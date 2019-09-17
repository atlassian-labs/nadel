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

    GraphQLInputValueDefinition getInputValueDefinition();

    GraphQLArgument getGraphQLArgument();

    TraverserContext<Node> getTraverserContext();

    Value getValue();

    Map<String, Object> getVariables();

}
