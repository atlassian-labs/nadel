package graphql.nadel.enginekt

import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.hooks.ServiceExecutionHooks

interface NadelEngineExecutionHooks : ServiceExecutionHooks {
    fun <T : NadelGenericHydrationInstruction> resolvePolymorphicHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode
    ): T
}