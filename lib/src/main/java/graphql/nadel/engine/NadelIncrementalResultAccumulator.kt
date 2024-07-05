package graphql.nadel.engine

import graphql.GraphQLError
import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.MutableJsonMap
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import graphql.normalized.incremental.NormalizedDeferredExecution

class NadelIncrementalResultAccumulator(
    private val operation: ExecutableNormalizedOperation,
) {
    data class DeferAccumulatorKey(
        val incrementalPayloadPath: List<Any>,
        val deferExecution: NormalizedDeferredExecution,
    )

    data class DeferAccumulator(
        val data: MutableJsonMap,
        val errors: MutableList<GraphQLError>,
    )

    private val deferAccumulators = mutableMapOf<DeferAccumulatorKey, DeferAccumulator>()

    private val extensions = mutableListOf<MutableJsonMap>()

    private val queryPathToExecutions: Map<NadelQueryPath, List<NormalizedDeferredExecution>> = operation.walkTopDown()
        .filter {
            it.deferredExecutions.isNotEmpty()
        }
        .groupBy(
            keySelector = {
                NadelQueryPath(it.parent?.listOfResultKeys ?: emptyList())
            },
            valueTransform = {
                it.deferredExecutions
            },
        )
        .mapValues { (_, values) ->
            values.flatten()
        }

    /**
     * todo: this doesn't account for type conditions
     */
    private val deferExecutionToResultKeys: Map<NormalizedDeferredExecution, List<String>> =
        operation.walkTopDown()
            .filter {
                it.deferredExecutions.isNotEmpty()
            }
            .flatMap { field ->
                field.deferredExecutions
                    .map { deferExecution ->
                        deferExecution to field
                    }
            }
            .groupBy(
                keySelector = { (deferExecution) ->
                    deferExecution
                },
                valueTransform = { (_, field) ->
                    field.resultKey
                }
            )

    fun getIncrementalPartialResult(hasNext: Boolean): DelayedIncrementalPartialResult? {
        val payloadsToEmit = deferAccumulators
            .filter {
                // i.e. complete
                it.value.data.size == deferExecutionToResultKeys[it.key.deferExecution]!!.size
            }
            .map { (key, accumulator) ->
                DeferPayload.newDeferredItem()
                    .data(accumulator.data)
                    .errors(accumulator.errors)
                    .path(key.incrementalPayloadPath)
                    .build()
            }

        if (payloadsToEmit.isEmpty()) {
            return null
        }

        // todo: handle extensions
        return DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()
            .incrementalItems(payloadsToEmit)
            .hasNext(hasNext)
            .build()
    }

    fun accumulate(result: DelayedIncrementalPartialResult) {
        result.incremental
            ?.forEach { payload ->
                when (payload) {
                    is DeferPayload -> {
                        val data = payload.getData<Map<String, Any?>?>()!! // todo: what happens if data is null?

                        val queryPath = NadelQueryPath.fromResultPath(payload.path)
                        val deferredExecutions =
                            queryPathToExecutions[queryPath]!! // todo: handle case where this wasn't picked up somehow

                        // todo: accumulate errors
                        deferredExecutions
                            .forEach { deferExecution ->
                                val accumulatorKey = DeferAccumulatorKey(
                                    incrementalPayloadPath = payload.path,
                                    deferExecution = deferExecution,
                                )

                                val resultKeys = deferExecutionToResultKeys[deferExecution]!!

                                val deferAccumulator = deferAccumulators.computeIfAbsent(accumulatorKey) {
                                    DeferAccumulator(
                                        mutableMapOf(),
                                        mutableListOf(),
                                    )
                                }

                                resultKeys.forEach { resultKey ->
                                    if (resultKey in data) {
                                        deferAccumulator.data[resultKey] = data[resultKey]
                                    }
                                }
                            }
                    }
                    else -> {
                        null
                    }
                }
            }
    }
}

/**
 * Similar to [java.io.File.walkTopDown] but for ENFs.
 */
fun ExecutableNormalizedOperation.walkTopDown(): Sequence<ExecutableNormalizedField> {
    return topLevelFields
        .asSequence()
        .flatMap {
            it.walkTopDown()
        }
}

/**
 * Similar to [java.io.File.walkTopDown] but for ENFs.
 */
fun ExecutableNormalizedField.walkTopDown(): Sequence<ExecutableNormalizedField> {
    return sequenceOf(this) + children.asSequence()
        .flatMap {
            it.walkTopDown()
        }
}
