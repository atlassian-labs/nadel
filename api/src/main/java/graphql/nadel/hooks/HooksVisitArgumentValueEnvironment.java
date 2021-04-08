package graphql.nadel.hooks;

import graphql.PublicApi;
import graphql.language.Node;
import graphql.language.Value;
import graphql.nadel.Service;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.util.TraverserContext;

import java.util.Map;

@PublicApi
public interface HooksVisitArgumentValueEnvironment {

    Value getValue();

    Service getService();

    Object getServiceContext();

    /**
     * The {@link GraphQLArgument} or {@link graphql.schema.GraphQLInputObjectField} in the underlying schema for the current value.
     *
     * @return never null
     */
    GraphQLInputValueDefinition getUnderlyingInputValueDefinition();

    /**
     * The {@link GraphQLArgument} or {@link graphql.schema.GraphQLInputObjectField} in the overall schema for the current value.
     *
     * @return this can be null if there is no corresponding overall input value definition
     */
    GraphQLInputValueDefinition getOverallInputValueDefinition();

    /**
     * The {@link GraphQLArgument} in the underlying schema this current value belongs to.
     *
     * @return never null
     */
    GraphQLArgument getUnderlyingGraphQLArgument();

    /**
     * The {@link GraphQLArgument} in the overall schema this current value belongs to.
     *
     * @return this can be null if there is no corresponding overall argument
     */
    GraphQLArgument getOverallGraphQLArgument();

    TraverserContext<Node> getTraverserContext();

    Map<String, Object> getVariables();

}
