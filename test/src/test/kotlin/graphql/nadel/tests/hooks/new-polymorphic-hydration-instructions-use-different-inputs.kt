package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `new-polymorphic-hydration-instructions-use-different-inputs` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T? {
                        val toString = parentNode.value.toString()

                        return when {
                            toString.contains("tall") -> instructions.single { it.actorFieldDef.name.contains("giraffe") }
                            toString.contains("good") -> instructions.single { it.actorFieldDef.name.contains("dog") }
                            toString.contains("cute") -> instructions.single { it.actorFieldDef.name.contains("cat") }
                            else -> throw UnsupportedOperationException("unknown boye")
                        }
                    }

                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        sourceId: JsonNode,
                        userContext: Any?,
                    ): T? {
                        throw UnsupportedOperationException()
                    }
                },
            )
    }
}
