package graphql.nadel

import graphql.incremental.DelayedIncrementalPartialResult
import org.reactivestreams.Publisher

sealed class ServiceExecutionResult @JvmOverloads constructor(
    val data: MutableMap<String, Any?> = LinkedHashMap(),
    val errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    val extensions: MutableMap<String, Any?> = LinkedHashMap(),
)

class NadelIncrementalServiceExecutionResult(
    serviceExecutionResult: ServiceExecutionResult,
    val incrementalItemPublisher: Publisher<DelayedIncrementalPartialResult>,
) : ServiceExecutionResult(serviceExecutionResult.data, serviceExecutionResult.errors, serviceExecutionResult.extensions)
