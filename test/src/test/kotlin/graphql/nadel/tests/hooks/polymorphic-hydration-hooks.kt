package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodePath
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.UseHook

private class PolymorphicHydrationHooks : NadelEngineExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper
    ): T? {

        val dataIdFieldName = if (instructions.any { it is NadelHydrationFieldInstruction })
            "hydration__data__dataId"
        else "batch_hydration__data__dataId"

        val dataIdValue = JsonNodeExtractor.getNodeAt(parentNode, JsonNodePath.root + dataIdFieldName)!!
            .value as String
        val actorFieldName = when {
            dataIdValue.startsWith("human", ignoreCase = true) -> "humanById"
            dataIdValue.startsWith("null", ignoreCase = true) -> null
            else -> "petById"
        }
        return instructions.singleOrNull { it.actorFieldDef.name == actorFieldName }
    }
}

open class PolymorphicHydrationTestHook : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHooks())
    }
}

@UseHook
class `solitary-polymorphic-hydration` : PolymorphicHydrationTestHook() {}

@UseHook
class `batch-polymorphic-hydration-with-interfaces` : PolymorphicHydrationTestHook() {}

@UseHook
class `batch-polymorphic-hydration-with-unions` : PolymorphicHydrationTestHook() {}

@UseHook
class `batch-polymorphic-hydration-with-rename` : PolymorphicHydrationTestHook() {}

@UseHook
class `batch-polymorphic-hydration-where-only-one-type-is-queried` : PolymorphicHydrationTestHook() {}

@UseHook
class `batch-polymorphic-hydration-when-hook-returns-null` : PolymorphicHydrationTestHook() {}

@UseHook
class `batch-polymorphic-hydration-when-hook-returns-null-1` : PolymorphicHydrationTestHook() {}

@UseHook
class `solitary-polymorphic-hydration-when-hook-returns-null` : PolymorphicHydrationTestHook() {}

@UseHook
class `batch-polymorphic-hydration-with-lots-of-renames` : PolymorphicHydrationTestHook() {}
