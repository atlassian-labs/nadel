package graphql.nadel

import graphql.incremental.DelayedIncrementalPartialResult
import org.reactivestreams.Publisher

sealed class ServiceExecutionResult @JvmOverloads constructor(
    val data: MutableMap<String, Any?> = LinkedHashMap(),
    val errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    val extensions: MutableMap<String, Any?> = LinkedHashMap(),
)

class NadelIncrementalServiceExecutionResult(
    data: MutableMap<String, Any?> = LinkedHashMap(),
    errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    extensions: MutableMap<String, Any?> = LinkedHashMap(),
    val incrementalItemPublisher: Publisher<DelayedIncrementalPartialResult>,
    val hasNext: Boolean,
) : ServiceExecutionResult(data, errors, extensions)

class NadelServiceExecutionResultImpl @JvmOverloads constructor(
    data: MutableMap<String, Any?> = LinkedHashMap(),
    errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    extensions: MutableMap<String, Any?> = LinkedHashMap(),
) : ServiceExecutionResult(data, errors, extensions)