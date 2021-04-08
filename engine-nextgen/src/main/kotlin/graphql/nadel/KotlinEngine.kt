package graphql.nadel

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture

class KotlinEngine(nadel: Nadel) : NadelExecutionEngine {
    override fun execute(input: NadelExecutionInput): CompletableFuture<ExecutionResult> {
        return GlobalScope.async {
            executeAsync(input)
        }.asCompletableFuture()
    }

    private suspend fun executeAsync(input: NadelExecutionInput): ExecutionResult {
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
