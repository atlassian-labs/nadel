package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.NadelEngineExecutionHooks
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private class PolymorphicHydrationHooks : NadelEngineExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
        userContext: Any?,
    ): T? {
        val dataIdFieldName = if (instructions.any { it is NadelHydrationFieldInstruction })
            "hydration__data__dataId"
        else "batch_hydration__data__dataId"

        val dataIdValue = (parentNode.value as JsonMap)[dataIdFieldName] as String
        val actorFieldName = when {
            dataIdValue.startsWith("human", ignoreCase = true) -> "humanById"
            dataIdValue.startsWith("null", ignoreCase = true) -> null
            else -> "petById"
        }
        return instructions.singleOrNull { it.actorFieldDef.name == actorFieldName }
    }
}

open class PolymorphicHydrationTestHook : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
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
