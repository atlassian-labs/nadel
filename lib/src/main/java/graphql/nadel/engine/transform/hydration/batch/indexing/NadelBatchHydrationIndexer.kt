package graphql.nadel.engine.transform.hydration.batch.indexing

import graphql.nadel.engine.transform.hydration.batch.NadelResolvedObjectBatch
import graphql.nadel.engine.transform.result.json.JsonNode

/**
 * Interface to define a process to index the result objects.
 */
internal interface NadelBatchHydrationIndexer {
    /**
     * The [sourceInput] is not always the key for the result index.
     *
     * e.g. for
     *
     * ```graphql
     * @hydrated(
     *     field: "jira.commentsById"
     *     arguments : [{ name:"input" value:"$source.context.jiraComment"}]
     *     inputIdentifiedBy : [
     *         {sourceId: "context.jiraComment.id", resultId: "id"}
     *     ]
     * )
     * ```
     *
     * The value of [sourceInput] is `context.jiraComment` but [getIndexKey] would return
     * the value of `context.jiraComment.id` instead.
     */
    fun getIndexKey(sourceInput: JsonNode): NadelBatchHydrationIndexKey

    fun getIndex(
        batches: List<NadelResolvedObjectBatch>,
    ): Map<NadelBatchHydrationIndexKey, JsonNode>
}
