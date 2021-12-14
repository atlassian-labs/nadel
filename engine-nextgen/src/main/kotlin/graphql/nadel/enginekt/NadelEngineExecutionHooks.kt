package graphql.nadel.enginekt

import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.hooks.ServiceExecutionHooks

interface NadelEngineExecutionHooks : ServiceExecutionHooks {
    fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper
    ): T?

    fun <T> partitionArgumentList(args: List<T>, instruction: NadelBatchHydrationFieldInstruction): List<List<T>> {
        return listOf(args)
    }
}