package graphql.nadel.engine.transformation;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

public class RemovedFieldData {
    private final GraphQLObjectType fieldContainer;
    private Field field;
    private GraphQLError graphQLError;
    private GraphQLOutputType outputType;

    public RemovedFieldData(Field field, GraphQLOutputType type, GraphQLObjectType fieldContainer, GraphQLError graphQLError) {
        this.field = field;
        this.graphQLError = graphQLError;
        this.outputType = type;
        this.fieldContainer = fieldContainer;
    }

    public GraphQLObjectType getFieldContainer() {
        return fieldContainer;
    }

    public Field getField() {
        return field;
    }


    public GraphQLError getGraphQLError() {
        return graphQLError;
    }

    public GraphQLOutputType getOutputType() {
        return outputType;
    }
}
