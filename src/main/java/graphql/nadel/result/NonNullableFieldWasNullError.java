package graphql.nadel.result;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorHelper;
import graphql.PublicApi;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;

import java.util.List;

import static graphql.schema.GraphQLTypeUtil.simplePrint;

/**
 * This is the base error that indicates that a non null field value was in fact null.
 */
@PublicApi
public class NonNullableFieldWasNullError implements GraphQLError {

    private final String message;
    private final List<Object> path;
//    private final NonNullableFieldWasNullError causedBy;

    public NonNullableFieldWasNullError(GraphQLNonNull nonNullType, ExecutionPath executionPath) {
//        this.message = exception.getMessage();
        GraphQLType graphQLType = GraphQLTypeUtil.unwrapOne(nonNullType);
        this.path = executionPath.toList();
        this.message = String.format("Cannot return null for non-nullable type: '%s' (%s)", simplePrint(graphQLType), path);
//        this.causedBy = causedBy;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public List<Object> getPath() {
        return path;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorType getErrorType() {
        return ErrorType.DataFetchingException;
    }

    @Override
    public String toString() {
        return "NonNullableFieldWasNullError{" +
                "message='" + message + '\'' +
                ", path=" + path +
                '}';
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return GraphqlErrorHelper.equals(this, o);
    }

    @Override
    public int hashCode() {
        return GraphqlErrorHelper.hashCode(this);
    }
}
