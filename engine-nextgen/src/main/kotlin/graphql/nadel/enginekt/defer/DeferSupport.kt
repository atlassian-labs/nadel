package graphql.nadel.enginekt.defer

import graphql.execution.reactive.SingleSubscriberPublisher
import graphql.language.Field
import graphql.language.StringValue
import graphql.nadel.defer.DeferredExecutionResult
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

class DeferSupport {
    private val deferDetected = AtomicBoolean(false)
    private val deferredCalls: Deque<DeferredCall<*>> = ConcurrentLinkedDeque()
    val publisher = SingleSubscriberPublisher<DeferredExecutionResult>()

    fun enqueue(deferredCall: DeferredCall<*>) {
        deferDetected.set(true)
        deferredCalls.offer(deferredCall)
    }

    private fun drainDeferredCalls() {
        if (deferredCalls.isEmpty()) {
            publisher.noMoreData()
            // Publish with "hasNext: false" ?
            return
        }
        val deferredCall = deferredCalls.pop()

        GlobalScope.launch {
            val executionResult = deferredCall.invoke()
            publisher.offer(executionResult)
            drainDeferredCalls()
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

            return normalizedOperation.getMergedField(topLevelField).fields
                .flatMap { obj: Field -> obj.directives }
                // TODO: resolve "if" expression
                .find { it.name == "defer" }
                ?.let { it.arguments }
                ?.let { args -> args.find { it.name == "label" } }
                ?.let { it.value }
                ?.let { it as? StringValue }
                ?.let { it.value }

        }
    }
}