package graphql.nadel.enginekt.defer

import graphql.execution.reactive.SingleSubscriberPublisher
import graphql.nadel.defer.DeferredExecutionResult
import graphql.normalized.DeferredNormalizedField
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

// TODO: belongs in GraphQL Java
/**
 * Implements https://github.com/graphql/graphql-spec/pull/742
 */
class DeferSupport {
    private val deferDetected = AtomicBoolean(false)
    private val deferredCalls: Deque<DeferredCall<*>> = ConcurrentLinkedDeque()
    val publisher = SingleSubscriberPublisher<DeferredExecutionResult>()

    fun enqueue(deferredCall: DeferredCall<*>) {
        deferDetected.set(true)
        deferredCalls.offer(deferredCall)
    }

    private fun drainDeferredCalls() {
        GlobalScope.launch {
            deferredCalls
                .map {
                    async {
                        val executionResult = it.invoke()
                        publisher.offer(executionResult)
                    }
                }
                .awaitAll()

            publisher.offer(DeferredExecutionResultImpl.newFinalExecutionResult())
            publisher.noMoreData()
        }
    }

    fun startDeferredCalls(): Publisher<DeferredExecutionResult> {
        drainDeferredCalls()
        return publisher
    }

    fun isDeferDetected(): Boolean {
        return deferDetected.get()
    }

    companion object {
        fun getDeferLabel(
            normalizedOperation: ExecutableNormalizedOperation,
            topLevelField: ExecutableNormalizedField
        ): String? {
//            normalizedOperation.normalizedFieldToMergedField.filter { it.key }

//            return normalizedOperation.getMergedField(topLevelField)
//                .fields
//                .flatMap { it.selectionSet?.selections ?: emptyList() }
//                .flatMap {
//                    when (it) {
//                        is InlineFragment -> it.directives
//                        is FragmentSpread -> it.directives
//                        else -> emptyList()
//                    }
//                }
//                .find { it.name == "defer" }
//                ?.let { it.arguments }
//                ?.let { args -> args.find { it.name == "label" } }
//                ?.let { it.value }
//                ?.let { it as? StringValue }
//                ?.let { it.value }


//            return normalizedOperation.getMergedField(topLevelField).fields
//                .flatMap { obj: Field -> obj.directives }
//                // TODO: resolve "if" expression
//                .find { it.name == "defer" }
//                ?.let { it.arguments }
//                ?.let { args -> args.find { it.name == "label" } }
//                ?.let { it.value }
//                ?.let { it as? StringValue }
//                ?.let { it.value }

            return when (topLevelField) {
                is DeferredNormalizedField -> topLevelField.deferredLabel
                else -> null
            }

        }
    }
}