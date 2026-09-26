package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationOperationPlanner.BackingQuery
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationOperationPlanner.Operation
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.toGraphQLError

/**
 * Maps errors from one packed shared backing operation to its recorded source consumers.
 *
 * Each pathful error is first routed to the aliased backing query that produced it, then fanned
 * out by object identifier. Request-level or unprovable errors are exposed once, and internal
 * backing aliases are never leaked to the client.
 */
internal class NadelSharedBatchHydrationErrorMapper {
    fun getOutputs(
        result: ServiceExecutionResult,
        operation: Operation,
    ): Map<Int, NadelCoalescedBatchHydrationOutput> {
        val queriesByRootResultKey = operation.queries.associateBy { query ->
            query.field.resultKey
        }
        val outputsByStableId = LinkedHashMap<Int, NadelCoalescedBatchHydrationOutput>()

        result.errors
            .filterNotNull()
            .forEach { error ->
                val rawPath = error["path"] as? List<*>
                val query = (rawPath?.firstOrNull() as? String)
                    ?.let(queriesByRootResultKey::get)
                val attributedErrors = if (query != null && rawPath != null) {
                    getAttributedErrors(
                        result = result,
                        query = query,
                        rawError = error,
                        backingErrorPath = rawPath,
                    )
                } else {
                    emptyList()
                }

                if (attributedErrors.isEmpty()) {
                    val fallbackStableId = query
                        ?.contributingConsumers
                        ?.minOf(NadelBatchHydrationCoalescingConsumer::stableId)
                        ?: operation.consumers.minOf(NadelBatchHydrationCoalescingConsumer::stableId)
                    outputsByStableId.add(
                        stableId = fallbackStableId,
                        output = NadelCoalescedBatchHydrationOutput(
                            instructions = listOf(
                                NadelResultInstruction.AddError(
                                    toGraphQLError(
                                        raw = error,
                                        // This backing path could not be tied to a source
                                        // occurrence, so exposing it would leak an internal field.
                                        path = null,
                                    ),
                                ),
                            ),
                        ),
                    )
                } else {
                    attributedErrors.forEach { (stableId, locatedError) ->
                        outputsByStableId.add(
                            stableId = stableId,
                            output = NadelCoalescedBatchHydrationOutput(
                                locatedErrors = listOf(locatedError),
                            ),
                        )
                    }
                }
            }

        return outputsByStableId
    }

    private fun getAttributedErrors(
        result: ServiceExecutionResult,
        query: BackingQuery,
        rawError: JsonMap,
        backingErrorPath: List<*>,
    ): List<Pair<Int, NadelCoalescedBatchHydrationError>> {
        val backingResultPath = query.resultPath.segments
        if (backingErrorPath.size <= backingResultPath.size ||
            backingResultPath.indices.any { index ->
                backingErrorPath[index] != backingResultPath[index]
            }
        ) {
            return emptyList()
        }

        val pathAfterBackingField = backingErrorPath.drop(backingResultPath.size)
        val resultIndex = (pathAfterBackingField.firstOrNull() as? Number)?.toInt()
            ?: return emptyList()
        if (resultIndex < 0) {
            return emptyList()
        }

        val internalAliases = getInternalAliases(query)
        val selectionPath = pathAfterBackingField
            .drop(1)
            .map { segment ->
                when {
                    segment == null -> return emptyList()
                    segment in internalAliases -> return emptyList()
                    else -> segment
                }
            }
        val resultObject = JsonNodes(result.data)
            .getNodesAt(
                queryPath = query.resultPath,
                flatten = false,
            )
            .emptyOrSingle()
            ?.value
            .let { values ->
                (values as? List<*>)?.getOrNull(resultIndex)
            } as? Map<*, *>
        val resultIndexKey = resultObject?.let { objectValue ->
            getResultIndexKey(
                query = query,
                resultObject = objectValue,
            )
        }
        val inputConsumers = if (resultIndexKey == null) {
            // Null bubbling may remove the artificial identifier. Object-ID matching cannot
            // infer the failed reordered item, so fan out within this argument chunk only.
            query.inputConsumers
        } else {
            query.inputConsumersByIndexKey[resultIndexKey].orEmpty()
        }

        return inputConsumers.map { inputConsumer ->
            inputConsumer.stableId to NadelCoalescedBatchHydrationError(
                rawError = rawError,
                subject = inputConsumer.sourceObject,
                relativePath = inputConsumer.relativePath + selectionPath,
            )
        }
    }

    private fun getResultIndexKey(
        query: BackingQuery,
        resultObject: Map<*, *>,
    ): NadelBatchHydrationIndexKey? {
        return NadelBatchHydrationIndexKey(
            query.lane.objectIdentifiers.map { objectId ->
                val resultKey = query.lane.aliasHelper.getResultKey(objectId.resultId)
                if (!resultObject.containsKey(resultKey)) {
                    return null
                }
                val resultId = resultObject[resultKey]
                    ?: return null
                JsonNode(resultId)
            },
        )
    }

    private fun getInternalAliases(query: BackingQuery): Set<String> {
        return query.lane.objectIdentifiers.mapTo(mutableSetOf()) { objectId ->
            query.lane.aliasHelper.getResultKey(objectId.resultId)
        }
    }

    private fun MutableMap<Int, NadelCoalescedBatchHydrationOutput>.add(
        stableId: Int,
        output: NadelCoalescedBatchHydrationOutput,
    ) {
        this[stableId] = getOrDefault(
            stableId,
            NadelCoalescedBatchHydrationOutput.EMPTY,
        ) + output
    }
}
