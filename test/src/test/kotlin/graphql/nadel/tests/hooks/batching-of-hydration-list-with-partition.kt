package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private class BatchHydrationHooks : NadelEngineExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper
    ): T {
        return instructions[0]
    }

    override fun <T> partitionBatchHydrationArgumentList(
        argumentValues: List<T>,
        instruction: NadelBatchHydrationFieldInstruction
    ): List<List<T>> {
        return argumentValues.groupBy { (it as String).substringBefore("/") }
            .values
            .toList()
    }
}

@UseHook
class `batching-of-hydration-list-with-partition` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(BatchHydrationHooks())
    }
}
