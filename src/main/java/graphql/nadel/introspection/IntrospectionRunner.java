package graphql.nadel.introspection;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.ExecutionContext;
import graphql.execution.nextgen.FieldSubSelection;

import java.util.concurrent.CompletableFuture;

/**
 * Nadel requires the ability to run Introspection queries over the presented schema.  This is not a downstream service call but rather a static
 * call onto presented schema types.
 *
 * Implementations of this interface are responsible for identifying introspection queries and then running them.
 */
public interface IntrospectionRunner {

    /**
     * Returns true if the query is in fact an introspection query
     *
     * @param executionContext  the query and its context that is being executed
     * @param fieldSubSelection the field sub selection in place
     *
     * @return true if this runner thinks this is a introspection query
     */
    boolean isIntrospectionQuery(ExecutionContext executionContext, FieldSubSelection fieldSubSelection);

    /**
     * Called to get a result for introspection.  This will be called if {@link #isIntrospectionQuery(graphql.execution.ExecutionContext, graphql.execution.nextgen.FieldSubSelection)} returns true
     *
     * @param executionContext  the execution context in play
     * @param fieldSubSelection the field sub selection in place
     * @param executionInput    the execution input being run
     *
     * @return a promise to a result
     */
    CompletableFuture<ExecutionResult> runIntrospection(ExecutionContext executionContext, FieldSubSelection fieldSubSelection, ExecutionInput executionInput);
}
