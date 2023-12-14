package graphql.nadel.engine.transform.hydration.batch.indexing

import graphql.nadel.engine.transform.hydration.batch.NadelResolvedObjectBatch
import graphql.nadel.engine.transform.result.json.JsonNode

/**
 * Interface to define a process to index the result objects.
 */
internal interface NadelBatchHydrationIndexer {
    fun getSourceKey(sourceInput: JsonNode): NadelBatchHydrationIndexKey

    fun getIndex(
        batches: List<NadelResolvedObjectBatch>,
    ): Map<NadelBatchHydrationIndexKey, JsonNode>
}
