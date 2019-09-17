package graphql.nadel.engine;

import graphql.language.Node;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.util.TraverserContext;

public interface HooksVisitArgumentValueEnvironment {

    GraphQLInputValueDefinition getInputValueDefinition();

    GraphQLArgument getGraphQLArgument();

    TraverserContext<Node> getContext();

}
