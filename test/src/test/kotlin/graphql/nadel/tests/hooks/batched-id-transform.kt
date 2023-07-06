package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.NadelEngineExecutionHooks
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `batched-id-transform` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionHooks(
                object : NadelEngineExecutionHooks {
                    override fun mapHydrationIds(
                        userContext: Any?,
                        ids: List<Any?>,
                        instruction: NadelGenericHydrationInstruction,
                    ): Map<Any?, List<Any?>> {
                        return ids
                            .associateWith {
                                listOf(it.toString() + "-MEMES-1", it.toString() + "-MEMES-2")
                            }
                    }

                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T {
                        return instructions.single()
                    }
                }
            )
    }
}
