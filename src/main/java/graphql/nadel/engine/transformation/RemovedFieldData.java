package graphql.nadel.engine.transformation;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

public class RemovedFieldData {
    private final GraphQLObjectType fieldContainerOverall;
    private Field field;
    private GraphQLError graphQLError;
    private GraphQLOutputType outputTypeOverall;

    public RemovedFieldData(Field field, GraphQLOutputType outputTypeOverall, GraphQLObjectType fieldContainerOverall, GraphQLError graphQLError) {
        this.field = field;
        this.graphQLError = graphQLError;
        this.outputTypeOverall = outputTypeOverall;
        this.fieldContainerOverall = fieldContainerOverall;
    }

    public GraphQLObjectType getFieldContainerOverall() {
        return fieldContainerOverall;
    }

    public Field getField() {
        return field;
    }


    public GraphQLError getGraphQLError() {
        return graphQLError;
    }

    public GraphQLOutputType getOutputTypeOverall() {
        return outputTypeOverall;
    }
}
