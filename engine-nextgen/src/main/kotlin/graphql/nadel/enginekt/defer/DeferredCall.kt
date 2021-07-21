package graphql.nadel.enginekt.defer

import graphql.ExecutionResult
import graphql.execution.ResultPath
import graphql.nadel.defer.DeferredExecutionResult

class DeferredCall(
    val path: ResultPath,
    val label: String,
    val call: suspend () -> ExecutionResult
) {
    suspend fun invoke(): DeferredExecutionResult {
        Thread.sleep(1000)
        return transformToDeferredResult(call.invoke())
    }

    private fun transformToDeferredResult(executionResult: ExecutionResult): DeferredExecutionResult {
        return DeferredExecutionResultImpl.newDeferredExecutionResult().from(executionResult)
            .path(path)
            .label(label)
            .build()
    }
}