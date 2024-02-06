package graphql.nadel.hooks

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.normalized.ExecutableNormalizedField
import java.util.concurrent.CompletableFuture

/**
 * These hooks allow you to change the way service execution happens
 */
interface NadelExecutionHooks {
    /**
     * Called per top level field for a service.  This allows you to create a "context" object that will be passed into further calls.
     *
     * @param params the parameters to this call
     * @return an async context object of your choosing
     */
    fun createServiceContext(params: CreateServiceContextParams): CompletableFuture<Any?> {
        return CompletableFuture.completedFuture(null)
    }

    /**
     * Called to resolve the service that should be used to fetch data for a field that uses dynamic service resolution.
     *
     *
     * There are 2 versions of this method. One passing an [ExecutionStepInfo], which is used by the CurrentGen
     * engine, and another passing [ExecutableNormalizedField], used by the NextGen engine. During the transition
     * between Current and NextGen, implementations of [NadelExecutionHooks] will have to implement both
     * versions of this method.
     *
     * @param services                  a collection of all services registered on Nadel
     * @param executableNormalizedField object containing data about the field being executed
     * @return the Service that should be used to fetch data for that field or an error that was raised when trying to resolve the service.
     */
    fun resolveServiceForField(
        services: List<Service>,
        executableNormalizedField: ExecutableNormalizedField,
    ): ServiceOrError? {
        return null
    }

    fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
        userContext: Any?,
    ): T? {
        return instructions.single()
    }

    fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        sourceInput: JsonNode,
        userContext: Any?,
    ): T? {
        return instructions.single()
    }

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
     * @param instruction user context supplied to the execution input
     * @return list of argument values partitioned accordingly. If no partitioning needed, return
     * `listOf(argumentValues)`
     */
    fun <T> partitionBatchHydrationArgumentList(
        argumentValues: List<T>,
        instruction: NadelBatchHydrationFieldInstruction,
        userContext: Any?,
    ): List<List<T>> {
        return listOf(argumentValues)
    }
}
