package graphql.nadel.incremental

import graphql.nadel.ServiceExecutionResult
import org.reactivestreams.Publisher

class IncrementalServiceExecutionResult(
    serviceExecutionResult: ServiceExecutionResult,
    val incrementalItemPublisher: Publisher<Any>,
) : ServiceExecutionResult(serviceExecutionResult.data, serviceExecutionResult.errors, serviceExecutionResult.extensions)

fun List<ServiceExecutionResult>.containsIncremental(): Boolean {
    return this.find { it is IncrementalServiceExecutionResult } != null
}

