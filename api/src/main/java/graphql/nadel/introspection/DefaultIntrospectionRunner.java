package graphql.nadel.introspection;

import graphql.ErrorClassification;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.Internal;
import graphql.execution.ExecutionContext;
import graphql.execution.nextgen.FieldSubSelection;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Internal
public class DefaultIntrospectionRunner implements IntrospectionRunner {

    private enum Errors implements ErrorClassification {
        MixedIntrospectionAndNormalFields
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean isIntrospectionQuery(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        if (isAllSystemFields(fieldSubSelection)) {
            return true;
        } else if (isMixedFields(fieldSubSelection)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CompletableFuture<ExecutionResult> runIntrospection(ExecutionContext executionContext, FieldSubSelection fieldSubSelection, ExecutionInput executionInput) {
        if (isMixedFields(fieldSubSelection)) {
            ExecutionResult executionResult = ExecutionResultImpl.newExecutionResult()
                    .errors(mkMixedIntrospectionError())
                    .build();
            return completedFuture(executionResult);
        }
        GraphQL graphQL = GraphQL.newGraphQL(executionContext.getGraphQLSchema()).build();
        return graphQL.executeAsync(executionInput);
    }

    private boolean isAllSystemFields(FieldSubSelection fieldSubSelection) {
        Set<String> fieldNames = fieldSubSelection.getSubFields().keySet();
        return fieldNames.stream().allMatch(name -> name.startsWith("__"));
    }

    private boolean isMixedFields(FieldSubSelection fieldSubSelection) {
        Set<String> fieldNames = fieldSubSelection.getSubFields().keySet();
        long systemFieldCount = fieldNames.stream().filter(name -> name.startsWith("__")).count();
        return systemFieldCount > 0 && systemFieldCount != fieldNames.size();
    }

    private List<GraphQLError> mkMixedIntrospectionError() {

        GraphQLError error = GraphqlErrorBuilder.newError()
                .errorType(Errors.MixedIntrospectionAndNormalFields)
                .message("You cannot mix Introspection __ system fields and normal fields in Nadel.  They MUST be mutually exclusive!")
                .build();

        return Collections.singletonList(error);
    }
}
