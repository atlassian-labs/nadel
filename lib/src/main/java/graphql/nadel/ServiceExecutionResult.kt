package graphql.nadel

import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalPayload
import org.reactivestreams.Publisher

sealed class ServiceExecutionResult {
    abstract val data: MutableMap<String, Any?>
    abstract val errors: MutableList<MutableMap<String, Any?>?>
    abstract val extensions: MutableMap<String, Any?>
}

data class NadelIncrementalServiceExecutionResult(
    override val data: MutableMap<String, Any?> = LinkedHashMap(),
    override val errors: MutableList<MutableMap<String, Any?>?> = ArrayList(),
    override val extensions: MutableMap<String, Any?> = LinkedHashMap(),
    val incremental: List<IncrementalPayload>?,
    val incrementalItemPublisher: Publisher<DelayedIncrementalPartialResult>,
    val hasNext: Boolean,
) : ServiceExecutionResult()

data class NadelServiceExecutionResultImpl @JvmOverloads constructor(
    override val data: MutableMap<String, Any?> = LinkedHashMap(),
    override val errors: MutableList<MutableMap<String, Any?>?> = ArrayList(),
    override val extensions: MutableMap<String, Any?> = LinkedHashMap(),
) : ServiceExecutionResult()
