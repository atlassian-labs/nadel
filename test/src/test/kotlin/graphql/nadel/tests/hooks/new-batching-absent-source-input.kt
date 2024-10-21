package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `new-batching-absent-source-input` : EngineTestHook {
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
                                it.backingService.name.startsWith(type, ignoreCase = true)
                            }
                    }

                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T? {
                        val nodeString = parentNode.value.toString()

                        return when  {
                            nodeString.contains("comment/") -> instructions.single {
                                it.backingFieldDef.name.contains("comment")
                            }
                            nodeString.contains("issue/") -> instructions.single {
                                it.backingFieldDef.name.contains("issue")
                            }
                            else -> null
                        }
                    }
                },
            )
    }
}
