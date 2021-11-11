package graphql.nadel.enginekt

import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.hooks.ServiceExecutionHooks

interface NadelEngineExecutionHooks : ServiceExecutionHooks {
    fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode
    ): T

    fun <T> partitionArgumentList(args: List<T>): List<List<T>>
}