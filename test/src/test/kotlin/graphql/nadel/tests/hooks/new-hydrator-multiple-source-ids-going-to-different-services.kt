package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `new-hydrator-multiple-source-ids-going-to-different-services` : EngineTestHook {
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder): NadelExecutionHints.Builder {
        return super.makeExecutionHints(builder)
            .newBatchHydrationGrouping { true }
    }

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        sourceId: JsonNode,
                        userContext: Any?,
                    ): T {
                        val type = (sourceId.value as String).substringBefore("/")

                        return instructions
                            .first {
                                it.actorService.name.startsWith(type, ignoreCase = true)
                            }
                    }
                },
            )
    }
}
