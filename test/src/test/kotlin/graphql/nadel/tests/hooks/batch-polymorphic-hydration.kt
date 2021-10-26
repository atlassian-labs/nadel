package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodePath
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.UseHook

@UseHook
class `batch-polymorphic-hydration` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(TestNadelEngineExecutionHooks())
    }
}

private class TestNadelEngineExecutionHooks : NadelEngineExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> resolvePolymorphicHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode
    ): T {

        val dataIdValue = JsonNodeExtractor.getNodeAt(parentNode, JsonNodePath.root + "batch_hydration__data__dataId")!!
            .value as String
        val actorFieldName = when {
            dataIdValue.startsWith("bar", ignoreCase = true) -> "barById"
            else -> "buzById"
        }
        return instructions.single { it.actorFieldDef.name == actorFieldName }
    }
}
