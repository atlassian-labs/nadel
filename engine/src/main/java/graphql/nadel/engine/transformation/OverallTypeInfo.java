package graphql.nadel.engine.transformation;

import graphql.Internal;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputValueDefinition;

@Internal
public class OverallTypeInfo {
    private final GraphQLFieldsContainer fieldsContainer;
    private final GraphQLFieldDefinition fieldDefinition;
    private final GraphQLArgument graphQLArgument;
    private final GraphQLInputValueDefinition graphQLInputValueDefinition;

    public OverallTypeInfo(GraphQLFieldsContainer fieldsContainer,
                           GraphQLFieldDefinition fieldDefinition,
                           GraphQLArgument graphQLArgument,
                           GraphQLInputValueDefinition graphQLInputValueDefinition) {
        this.fieldsContainer = fieldsContainer;
        this.fieldDefinition = fieldDefinition;
        this.graphQLArgument = graphQLArgument;
        this.graphQLInputValueDefinition = graphQLInputValueDefinition;
    }

    public GraphQLFieldsContainer getFieldsContainer() {
        return fieldsContainer;
    }


    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public GraphQLArgument getGraphQLArgument() {
        return graphQLArgument;
    }

    public GraphQLInputValueDefinition getGraphQLInputValueDefinition() {
        return graphQLInputValueDefinition;
    }
}

