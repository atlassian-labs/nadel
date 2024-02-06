package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `new-batch-hydration-instruction-hook-returns-null` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): Nothing {
                        throw UnsupportedOperationException("should not run")
                    }

                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        sourceInput: JsonNode,
                        userContext: Any?,
                    ): T? {
                        return if (sourceInput.value.toString().contains("NULL", ignoreCase = true)) {
                            null
                        } else {
                            instructions.single()
                        }
                    }
                },
            )
    }
}
