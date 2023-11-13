package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private class BatchHydrationHooks : NadelExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
        userContext: Any?,
    ): T {
        return instructions[0]
    }

    override fun <T> partitionBatchHydrationArgumentList(
        argumentValues: List<T>,
        instruction: NadelBatchHydrationFieldInstruction,
        userContext: Any?,
    ): List<List<T>> {
        return argumentValues.groupBy { (it as String).substringBefore("/") }
            .values
            .toList()
    }
}

@UseHook
class `batching-of-hydration-list-with-partition` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(BatchHydrationHooks())
    }
}
