package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.language.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface NadelExecutionEngine {
    @NotNull
    CompletableFuture<ExecutionResult> execute(
            @NotNull ExecutionInput executionInput,
            @NotNull Document queryDocument,
            @Nullable InstrumentationState instrumentationState,
            @NotNull NadelExecutionParams nadelExecutionParams
    );
}
