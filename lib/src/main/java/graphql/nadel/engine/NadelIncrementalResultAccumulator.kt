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
    private val deferExecutionToFields: Map<NormalizedDeferredExecution, List<ExecutableNormalizedField>> =
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
                    field
                },
            )
            .filterValues {
                it.isNotEmpty()
            }
            .mapValues { (_, fields) ->
                val topLevel = fields.minOf {
                    it.level
                }
                fields.filter {
                    it.level == topLevel
                }
            }

    fun getIncrementalPartialResult(hasNext: Boolean): DelayedIncrementalPartialResult? {
        val readyAccumulators = deferAccumulators
            .filter {
                // i.e. complete
                it.value.data.size == deferExecutionToFields[it.key.deferExecution]!!.size
            }
            .onEach {
                deferAccumulators.remove(it.key)
            }

        if (readyAccumulators.isEmpty()) {
            return null
        }

        val payloadsToEmit = readyAccumulators
            .map { (key, accumulator) ->
                DeferPayload.newDeferredItem()
                    .data(accumulator.data)
                    .errors(accumulator.errors)
                    .path(key.incrementalPayloadPath)
                    .label(key.deferExecution.label)
                    .build()
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
                        accumulate(payload)
                    }
                }
            }
    }

    private fun accumulate(payload: DeferPayload) {
        val data = payload.getData<Map<String, Any?>?>()
            ?: return

        val queryPath = NadelQueryPath.fromResultPath(payload.path)
        val deferredExecutions = queryPathToExecutions[queryPath]
            ?: return

        deferredExecutions
            .asSequence()
            .filter {
                payload.label == it.label
            }
            .forEachIndexed { index, deferExecution ->
                val accumulatorKey = DeferAccumulatorKey(
                    incrementalPayloadPath = payload.path,
                    deferExecution = deferExecution,
                )

                val deferAccumulator = deferAccumulators.computeIfAbsent(accumulatorKey) {
                    DeferAccumulator(
                        data = mutableMapOf(),
                        errors = mutableListOf(),
                    )
                }

                deferExecutionToFields[deferExecution]!!.forEach { field ->
                    if (field.resultKey in data) {
                        deferAccumulator.data[field.resultKey] = data[field.resultKey]
                    }
                }

                // todo: there's no good way to determine which defer execution a payload belongs to
                if (index == 0) {
                    deferAccumulator.errors.addAll(payload.errors ?: emptyList())
                }
            }
    }
}

/**
 * Similar to [java.io.File.walkTopDown] but for ENFs.
 */
private fun ExecutableNormalizedOperation.walkTopDown(): Sequence<ExecutableNormalizedField> {
    return topLevelFields
        .asSequence()
        .flatMap {
            it.walkTopDown()
        }
}

/**
 * Similar to [java.io.File.walkTopDown] but for ENFs.
 */
private fun ExecutableNormalizedField.walkTopDown(): Sequence<ExecutableNormalizedField> {
    return sequenceOf(this) + children.asSequence()
        .flatMap {
            it.walkTopDown()
        }
}
