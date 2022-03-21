package graphql.nadel.engine

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.ServiceExecutionHooks

interface NadelEngineExecutionHooks : ServiceExecutionHooks {
    fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper
    ): T?

    /**
     * This method should be used when the list of hydration arguments needs to be split in batches. The batches will be
     * executed separately. One example is partitioning the list of arguments when different arguments cannot be
     * executed together in the same GraphQL query.
     *
     * Example with a sharded system when every argument can only be passed to a corresponding shard:
     * when
     * ```
     *  argumentValues = ["shard-0/issue-0", "shard-0/issue-1", "shard-1/issue-0", "shard-1/issue-1"]
     * ```
     * are passed to this method, they can be grouped together by some ruled derived from the argument values: name of
     * the shard in this case:
     * ```
     *  return [["shard-0/issue-0", "shard-0/issue-1"], ["shard-1/issue-0", "shard-1/issue-1"]]}
     * ```
     * This will result in 2 batch hydration calls made (2 queries executed against the underlying service):
     *  * batch hydration with arguments `"shard-0/issue-0", "shard-0/issue-1"`
     *  * batch hydration with arguments `"shard-1/issue-0", "shard-1/issue-1"`
     *
     * @param argumentValues list of argument values for this batch hydration
     * @param instruction batch hydration instruction for this hydration
     * @return list of argument values partitioned accordingly. If no partitioning needed, return
     * `listOf(argumentValues)`
     */
    fun <T> partitionBatchHydrationArgumentList(
        argumentValues: List<T>,
        instruction: NadelBatchHydrationFieldInstruction
    ): List<List<T>> {
        return listOf(argumentValues)
    }
}
