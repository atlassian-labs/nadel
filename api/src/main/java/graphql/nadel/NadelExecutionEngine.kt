package graphql.nadel

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import java.util.concurrent.CompletableFuture

interface NadelExecutionEngine {
    fun execute(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?,
        nadelExecutionParams: NadelExecutionParams,
    ): CompletableFuture<ExecutionResult>

    fun close() {
    }
}
