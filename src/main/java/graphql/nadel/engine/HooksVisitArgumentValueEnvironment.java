package graphql.nadel.engine;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;

public interface HooksVisitArgumentValueEnvironment {

    GraphQLInputValueDefinition getInputValueDefinition();

    GraphQLArgument getGraphQLArgument();


}
