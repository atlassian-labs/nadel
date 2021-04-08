package graphql.nadel;

import graphql.ExecutionResult;

import java.util.concurrent.CompletableFuture;

public interface NadelExecutionEngine {
    CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput);
}
