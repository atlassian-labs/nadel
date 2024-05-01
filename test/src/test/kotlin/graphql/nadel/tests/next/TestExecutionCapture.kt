package graphql.nadel.tests.next

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalExecutionResult
import graphql.incremental.IncrementalExecutionResultImpl
import graphql.nadel.engine.util.JsonMap
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher

class TestExecutionCapture {
    private val _calls = mutableListOf<Call>()
    val calls: List<Call>
        get() = _calls

    var result: ExecutionResult? = null
        private set

    private val _delayedResults = mutableListOf<DelayedIncrementalPartialResult>()
    val delayedResults: List<DelayedIncrementalPartialResult>
        get() = _delayedResults

    data class Call(
        val query: String,
        val variables: JsonMap,
        val result: ExecutionResult,
        val delayedResults: List<DelayedIncrementalPartialResult>,
    )

    fun capture(
        query: String,
        variables: JsonMap,
        result: ExecutionResult,
    ): ExecutionResult {
        val delayedResults = mutableListOf<DelayedIncrementalPartialResult>()

        _calls.add(
            Call(
                query = query,
                variables = variables,
                result = result,
                delayedResults = delayedResults,
            ),
        )

        return spyOnIncrementalResults(result) {
            delayedResults.add(it)
        }
    }

    fun capture(result: ExecutionResult): ExecutionResult {
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
}
