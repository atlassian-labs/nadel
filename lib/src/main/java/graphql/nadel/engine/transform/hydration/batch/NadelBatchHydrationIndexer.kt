package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.AnyIterable
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.singleOfType

@Suppress("DataClassPrivateConstructor") // Whatever, no matter
internal data class NadelBatchHydrationIndexKey private constructor(private val key: Any?) {
    constructor(node: JsonNode?) : this(key = node)
    constructor(nodes: List<JsonNode?>) : this(key = nodes)
}

/**
 * Interface to define a process to index the result objects.
 */
internal interface NadelBatchHydrationIndexer {
    fun getSourceKey(sourceInput: JsonNode): NadelBatchHydrationIndexKey

    fun getIndex(
        batches: List<NadelResolvedObjectBatch>,
    ): Map<NadelBatchHydrationIndexKey, JsonNode>
}

internal class NadelBatchHydrationObjectIdentifiedIndexer(
    private val aliasHelper: NadelAliasHelper,
    private val instruction: NadelBatchHydrationFieldInstruction,
    private val strategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers,
) : NadelBatchHydrationIndexer {
    constructor(
        aliasHelper: NadelAliasHelper,
        instruction: NadelBatchHydrationFieldInstruction,
        strategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ) : this(
        aliasHelper = aliasHelper,
        instruction = instruction,
        strategy = NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(listOf(strategy)),
    )

    override fun getSourceKey(sourceInput: JsonNode): NadelBatchHydrationIndexKey {
        // todo: bake this into the instruction
        val sourceInputPath = instruction.actorInputValueDefs
            .asSequence()
            .map { it.valueSource }
            .singleOfType<NadelHydrationActorInputDef.ValueSource.FieldResultValue>()
            .queryPathToField

        return NadelBatchHydrationIndexKey(
            strategy.objectIds
                .map { identifiedBy ->
                    // e.g. sourceInputPath is context.comment
                    // e.g. identifiedBy.sourceId is context.comment.id
                    // or
                    // e.g. sourceInputPath is commentId
                    // e.g. identifiedBy.sourceId is commentId
                    if (identifiedBy.sourceId == sourceInputPath) {
                        sourceInput
                    } else {
                        val pathFromSourceInput = identifiedBy.sourceId.drop(
                            // Counts the common prefix to remove
                            n = (identifiedBy.sourceId.segments)
                                .asSequence()
                                .zip(sourceInputPath.segments.asSequence())
                                .takeWhile { (a, b) -> a == b }
                                .count(),
                        )

                        JsonNodeExtractor.getNodesAt(sourceInput, pathFromSourceInput, flatten = false)
                            .emptyOrSingle()
                    }
                },
        )
    }

    override fun getIndex(
        batches: List<NadelResolvedObjectBatch>,
    ): Map<NadelBatchHydrationIndexKey, JsonNode> {
        return batches
            .asSequence()
            .flatMap { batch ->
                JsonNodeExtractor
                    .getNodesAt(batch.result.data, instruction.queryPathToActorField, flatten = true)
                    // Ignore nulls in result
                    .filter {
                        it.value != null
                    }
            }
            .groupBy { node ->
                @Suppress("UNCHECKED_CAST")
                NadelBatchHydrationIndexKey(
                    strategy.objectIds
                        .map { objectId ->
                            val resultKey = aliasHelper.getResultKey(objectId.resultId)
                            JsonNode((node.value as MutableJsonMap).remove(resultKey))
                        }
                )
            }
            .mapValues { (_, values) ->
                // todo: stop doing stupid here
                values.first()
            }
    }
}

internal class NadelBatchHydrationIndexBasedIndexer(
    private val instruction: NadelBatchHydrationFieldInstruction,
) : NadelBatchHydrationIndexer {
    override fun getSourceKey(sourceInput: JsonNode): NadelBatchHydrationIndexKey {
        return NadelBatchHydrationIndexKey(sourceInput)
    }

    override fun getIndex(
        batches: List<NadelResolvedObjectBatch>,
    ): Map<NadelBatchHydrationIndexKey, JsonNode> {
        return batches
            .flatMap { batch ->
                val data = JsonNodeExtractor
                    .getNodesAt(batch.result.data, instruction.queryPathToActorField, flatten = false)

                if (data.emptyOrSingle()?.value == null) {
                    emptyList()
                } else {
                    JsonNodeExtractor
                        .getNodesAt(batch.result.data, instruction.queryPathToActorField)
                        .asSequence()
                        .flatMap {
                            when (val value = it.value) {
                                is AnyIterable -> value.asSequence()
                                else -> sequenceOf(value)
                            }
                        }
                        .map {
                            JsonNode(it)
                        }
                        .toList()
                        .let { resolvedObjects ->
                            // Must be 1-1 with inputs
                            require(resolvedObjects.size == batch.sourceInputs.size) {
                                "If you use indexed hydration then you MUST follow a contract where the resolved nodes matches the size of the input arguments"
                            }
                            batch.sourceInputs.zip(resolvedObjects)
                        }
                }
            }
            .groupBy(
                keySelector = { (sourceId, _) ->
                    NadelBatchHydrationIndexKey(sourceId)
                },
                valueTransform = { (_, resolvedObject) ->
                    resolvedObject
                },
            )
            .mapValues { (_, values) ->
                // It's possible there are multiple, but there really shouldn't be
                values.first()
            }
    }
}
