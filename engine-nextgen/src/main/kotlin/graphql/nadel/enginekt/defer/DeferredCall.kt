package graphql.nadel.enginekt.defer

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.ResultPath
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.defer.DeferredExecutionResult

abstract class DeferredCall<ResultType>(
    val path: ResultPath,
    val label: String,
    val call: suspend () -> ResultType
) {
    suspend fun invoke(): DeferredExecutionResult {
        return transformToDeferredResult(call.invoke())
    }

    abstract fun transformToDeferredResult(result: ResultType): DeferredExecutionResult
}

class ExecutionDeferredCall(
    path: ResultPath,
    label: String,
    call: suspend () -> ExecutionResult
): DeferredCall<ExecutionResult>(path, label, call) {
    override fun transformToDeferredResult(result: ExecutionResult): DeferredExecutionResult {
        return LabeledDeferredExecutionResult.newDeferredExecutionResult().from(result)
            .path(path)
            .label(label)
            .build()
    }
}

class ServiceDeferredCall(
    path: ResultPath,
    label: String,
    call: suspend () -> ServiceExecutionResult
): DeferredCall<ServiceExecutionResult>(path, label, call) {
    override fun transformToDeferredResult(result: ServiceExecutionResult): DeferredExecutionResult {
        val executionResult = ExecutionResultImpl.newExecutionResult()
            .data(result.data)
            // TODO: Transform and set errors
//            .errors(serviceExecutionResult.errors.map { GraphqlErrorBuilder.newError().message(it).build() })
            .build()


        return LabeledDeferredExecutionResult.newDeferredExecutionResult().from(executionResult)
            .path(path)
            .label(label)
            .build()
    }
}

