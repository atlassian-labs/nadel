package graphql.nadel.engine.transformation;

import graphql.GraphQLError;
import graphql.execution.ExecutionPath;
import graphql.language.Field;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

public class RemovedFieldData {
    private final GraphQLObjectType fieldContainer;
    private Field field;
    private ExecutionPath executionPath;
    private GraphQLError graphQLError;
    private GraphQLOutputType outputType;

    public RemovedFieldData(Field field, ExecutionPath executionPath, GraphQLOutputType type, GraphQLObjectType fieldContainer, GraphQLError graphQLError) {
        this.field = field;
        this.executionPath = executionPath;
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

    public ExecutionPath getExecutionPath() {
        return executionPath;
    }

    public GraphQLError getGraphQLError() {
        return graphQLError;
    }

    public GraphQLOutputType getOutputType() {
        return outputType;
    }
}
