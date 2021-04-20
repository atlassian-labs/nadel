package graphql.nadel

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture

class KotlinEngine(nadel: Nadel) : NadelExecutionEngine {
    override fun execute(
        executionInput: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?,
        nadelExecutionParams: NadelExecutionParams
    ): CompletableFuture<ExecutionResult> {
        return GlobalScope.async {
            executeCoroutine(executionInput, queryDocument, instrumentationState)
        }.asCompletableFuture()
    }

    private suspend fun executeCoroutine(
        input: ExecutionInput,
        queryDocument: Document,
        instrumentationState: InstrumentationState?
    ): ExecutionResult {
        return ExecutionResultImpl.newExecutionResult()
            .build()
    }

    companion object {
        @JvmStatic
        fun newNadel(): Nadel.Builder {
            return Nadel.Builder().engineFactory(::KotlinEngine)
        }
    }
}
