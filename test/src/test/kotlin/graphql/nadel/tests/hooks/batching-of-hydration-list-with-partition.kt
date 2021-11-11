package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.UseHook

private class BatchHydrationHooks : NadelEngineExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode
    ): T {
        return instructions[0]
    }

    override fun <T> partitionArgumentList(args: List<T>): List<List<T>> {
        val partition = args.partition { (it as String).endsWith("2") || (it as String).endsWith("4") }
        return listOf(partition.first, partition.second)
    }
}

@UseHook
class `batching-of-hydration-list-with-partition` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(BatchHydrationHooks())
    }
}