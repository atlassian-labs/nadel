package graphql.nadel;

import graphql.ExecutionResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface NadelExecutionEngine {
    @NotNull
    CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput);
}
