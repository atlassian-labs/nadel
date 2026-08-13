package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationOperationPlanner.BackingQuery
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationOperationPlanner.Operation
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultMutation
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.toGraphQLError

/**
 * Maps backing errors from one shared operation back to the exact consumer occurrences recorded
 * by the planner.
 *
 * Keeping this separate from execution makes the provenance boundary explicit. The mapper
 * refines an operation's recorded consumers when the returned identifier survives; if GraphQL
 * null bubbling removes that identifier, it conservatively retains every recorded occurrence
 * rather than inventing an input-to-result ordering.
 */
internal class NadelSharedBatchHydrationErrorMapper {
    fun getMutations(
        result: ServiceExecutionResult,
        operation: Operation,
    ): Map<Int, List<NadelResultMutation>> {
        val queries = operation.queries
        val queryByRootAlias = queries.associateBy { query ->
            query.field.resultKey
        }
        val internalAliases = queries
            .flatMapTo(mutableSetOf(), ::getInternalAliases)
        val mutationsByInvocationId =
            LinkedHashMap<Int, MutableList<NadelResultMutation>>()

        result.errors
            .filterNotNull()
            .forEach { error ->
                val rawPath = error["path"] as? List<*>
                val query = (rawPath?.firstOrNull() as? String)
                    ?.let(queryByRootAlias::get)
                val attributedMutations = if (query == null) {
                    emptyList()
                } else {
                    getAttributedMutations(
                        result = result,
                        query = query,
                        rawError = error,
                        backingErrorPath = rawPath,
                    )
                }

                if (attributedMutations.isEmpty()) {
                    // A request-level error, or a path we cannot prove belongs to a concrete
                    // source occurrence, is exposed exactly once. Never leak a shared root
                    // alias merely because attribution failed.
                    val fallbackInvocationId = query
                        ?.contributingConsumers
                        ?.minOf { consumer -> consumer.invocation.id }
                        ?: operation.consumers.minOf { consumer -> consumer.invocation.id }
                    val sanitizedPath = rawPath?.takeUnless { path ->
                        path.any { segment ->
                            segment in internalAliases
                        }
                    }
                    mutationsByInvocationId
                        .getOrPut(fallbackInvocationId, ::mutableListOf)
                        .add(
                            NadelResultMutation.Instruction(
                                NadelResultInstruction.AddError(
                                    toGraphQLError(
                                        raw = error,
                                        path = sanitizedPath,
                                    ),
                                ),
                            ),
                        )
                } else {
                    attributedMutations.forEach { (invocationId, mutation) ->
                        mutationsByInvocationId
                            .getOrPut(invocationId, ::mutableListOf)
                            .add(mutation)
                    }
                }
            }

        return mutationsByInvocationId
    }

    /**
     * Maps one pathful backing error to every client occurrence that consumed the failing
     * returned object. Every step must be attributable; otherwise the caller deliberately
     * treats the error as request-level.
     */
    private fun getAttributedMutations(
        result: ServiceExecutionResult,
        query: BackingQuery,
        rawError: JsonMap,
        backingErrorPath: List<*>,
    ): List<Pair<Int, NadelResultMutation.AddErrorAt>> {
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

        val selectionPath = pathAfterBackingField
            .drop(1)
            .map { segment ->
                when {
                    segment == null -> return emptyList()
                    segment in getInternalAliases(query) -> return emptyList()
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
            // A non-null child can null-bubble the complete returned object, including the
            // artificial identifier. Object-identifier matching cannot then prove which
            // reordered item failed, so conservatively attribute the error to every source
            // occurrence represented by this backing query.
            query.inputConsumers
        } else {
            query.inputConsumersByIndexKey[resultIndexKey].orEmpty()
        }

        return inputConsumers.map { inputConsumer ->
            inputConsumer.invocationId to NadelResultMutation.AddErrorAt(
                rawError = rawError,
                subject = inputConsumer.sourceOccurrence,
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

    private fun getInternalAliases(
        query: BackingQuery,
    ): Set<String> {
        return buildSet {
            add(query.field.resultKey)
            query.lane.objectIdentifiers.forEach { objectId ->
                add(query.lane.aliasHelper.getResultKey(objectId.resultId))
            }
        }
    }
}
