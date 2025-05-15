package graphql.nadel.engine.transform.hydration.batch.indexing

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy.NO_CAST
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy.TO_STRING
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.batch.NadelResolvedObjectBatch
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.singleOfType

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

    override fun getIndexKey(sourceInput: JsonNode): NadelBatchHydrationIndexKey {
        // todo: bake this into the instruction
        val sourceInputPath = instruction.backingFieldArguments
            .asSequence()
            .map { it.valueSource }
            .singleOfType<NadelHydrationArgument.ValueSource.FieldResultValue>()
            .queryPathToField

        return NadelBatchHydrationIndexKey(
            strategy.objectIds
                .map { identifiedBy ->
                    // e.g. sourceInputPath is context.comment
                    // e.g. identifiedBy.sourceId is context.comment.id
                    // or
                    // e.g. sourceInputPath is commentId
                    // e.g. identifiedBy.sourceId is commentId
                    val sourceObjectId = if (identifiedBy.sourceId == sourceInputPath) {
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

                    if (sourceObjectId != null) {
                        smartCastKey(sourceObjectId, identifiedBy.sourceIdCast)
                    } else {
                        null
                    }
                }
        )
    }

    private fun smartCastKey(
        objectId: JsonNode,
        identifiedBy: NadelObjectIdentifierCastingStrategy,
    ): JsonNode {
        return when (identifiedBy) {
            TO_STRING -> objectId.copy(value = objectId.value.toString())
            NO_CAST -> objectId
        }
    }

    override fun getIndex(
        batches: List<NadelResolvedObjectBatch>,
    ): Map<NadelBatchHydrationIndexKey, JsonNode> {
        return batches
            .asSequence()
            .flatMap { batch ->
                JsonNodeExtractor.getNodesAt(batch.result.data, instruction.queryPathToBackingField, flatten = true)
                    // Ignore nulls in result
                    .filter {
                        it.value != null
                    }
            }
            .groupBy { node ->
                @Suppress("UNCHECKED_CAST")
                (NadelBatchHydrationIndexKey(
                    strategy.objectIds
                        .map { objectId ->
                            val resultKey = aliasHelper.getResultKey(objectId.resultId)
                            JsonNode(
                                (node.value as MutableJsonMap).remove(
                                    resultKey
                                )
                            )
                        }
                ))
            }
            .mapValues { (_, values) ->
                // todo: stop doing stupid here
                values.first()
            }
    }
}
