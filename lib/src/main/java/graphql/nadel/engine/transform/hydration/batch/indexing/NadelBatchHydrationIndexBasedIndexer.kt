package graphql.nadel.engine.transform.hydration.batch.indexing

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.transform.hydration.batch.NadelResolvedObjectBatch
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.AnyIterable
import graphql.nadel.engine.util.emptyOrSingle

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
                val data =
                    JsonNodeExtractor.getNodesAt(batch.result.data, instruction.queryPathToActorField, flatten = false)

                if (data.emptyOrSingle()?.value == null) {
                    emptyList()
                } else {
                    JsonNodeExtractor.getNodesAt(batch.result.data, instruction.queryPathToActorField)
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
