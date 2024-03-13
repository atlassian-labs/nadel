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
    val hasNext: Boolean,
) : ServiceExecutionResult(serviceExecutionResult.data, serviceExecutionResult.errors, serviceExecutionResult.extensions)

class NadelServiceExecutionResultImpl @JvmOverloads constructor(
    data: MutableMap<String, Any?> = LinkedHashMap(),
    errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    extensions: MutableMap<String, Any?> = LinkedHashMap(),
) : ServiceExecutionResult(data, errors, extensions)