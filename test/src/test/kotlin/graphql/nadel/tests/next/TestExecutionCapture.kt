package graphql.nadel.tests.next

import com.fasterxml.jackson.module.kotlin.convertValue
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalExecutionResult
import graphql.incremental.IncrementalExecutionResultImpl
import graphql.nadel.NadelExecutionInput
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.jsonObjectMapper
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import java.util.Collections

class TestExecutionCapture {
    private val _calls = synchronizedMutableListOf<Call>()
    val calls: List<Call>
        get() = _calls

    var executionInput: NadelExecutionInput? = null
        private set

    var result: ExecutionResult? = null
        private set

    private val _delayedResults = synchronizedMutableListOf<DelayedIncrementalPartialResult>()
    val delayedResults: List<DelayedIncrementalPartialResult>
        get() = _delayedResults

    data class Call(
        val service: String,
        val query: String,
        val variables: JsonMap,
        val result: JsonMap,
        val delayedResults: List<DelayedIncrementalPartialResult>,
    )

    fun capture(
        service: String,
        query: String,
        variables: JsonMap,
        result: ExecutionResult,
    ): ExecutionResult {
        val delayedResults = synchronizedMutableListOf<DelayedIncrementalPartialResult>()

        _calls.add(
            Call(
                service = service,
                query = query,
                variables = variables,
                result = deepClone(result),
                delayedResults = delayedResults,
            ),
        )

        return spyOnIncrementalResults(result) {
            delayedResults.add(it)
        }
    }

    fun capture(executionInput: NadelExecutionInput, result: ExecutionResult): ExecutionResult {
        this.executionInput = executionInput
        this.result = result
        return spyOnIncrementalResults(result) {
            _delayedResults.add(it)
        }
    }

    private fun spyOnIncrementalResults(
        result: ExecutionResult,
        onDelayedResult: (DelayedIncrementalPartialResult) -> Unit,
    ): ExecutionResult {
        return if (result is IncrementalExecutionResult) {
            IncrementalExecutionResultImpl.fromExecutionResult(result)
                // Must be supplied 1-1 as fromExecutionResult only copies from basic ExecutionResult
                .incremental(result.incremental)
                // Spy on incremental publisher and capture the subsequent calls
                .incrementalItemPublisher(
                    result.incrementalItemPublisher
                        .asFlow()
                        .onEach {
                            onDelayedResult(it)
                        }
                        .asPublisher()
                )
                .build()
        } else {
            result
        }
    }

    /**
     * The issue is that Nadel manipulates the underlying service response.
     * We need to capture the original response before that happens.
     */
    private fun deepClone(result: ExecutionResult): JsonMap {
        return jsonObjectMapper.convertValue(result.toSpecification()!!)
    }

    /**
     * Overrides the default one to mark it as deprecated because you probably
     * want the synchronized one anyway.
     */
    @Deprecated(
        message = "Use the synchronized version",
        replaceWith = ReplaceWith("synchronizedMutableListOf<T>(*elements)"),
    )
    private fun <T> mutableListOf(vararg elements: T): MutableList<T> {
        return kotlin.collections.mutableListOf(*elements)
    }

    private fun <T> synchronizedMutableListOf(vararg elements: T): MutableList<T> {
        return Collections.synchronizedList(kotlin.collections.mutableListOf(*elements))
    }
}
