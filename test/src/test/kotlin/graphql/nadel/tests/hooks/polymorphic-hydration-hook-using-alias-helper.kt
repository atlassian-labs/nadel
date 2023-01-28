package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.NadelEngineExecutionHooks
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.hydration.EffectFieldArgumentDef
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private class PolymorphicHydrationHookUsingAliasHelper : NadelEngineExecutionHooks {

    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
        userContext: Any?,
    ): T? {
        return instructions.firstOrNull {
            val (_, _, valueSource) = it.effectFieldArgDefs.single()
            if (valueSource !is EffectFieldArgumentDef.ValueSource.FromResultValue) {
                return@firstOrNull false
            }
            val actorFieldName = valueSource.fieldDefinition.name
            val targetFieldName = aliasHelper.getResultKey(actorFieldName)
            val hydrationArgumentValue = (parentNode.value as JsonMap)[targetFieldName]
            hydrationInstructionMatchesArgumentValue(it, hydrationArgumentValue as String)
        }
    }

    private fun <T : NadelGenericHydrationInstruction> hydrationInstructionMatchesArgumentValue(
        instruction: T,
        hydrationArgumentValue: String,
    ): Boolean {
        return instruction.effectFieldDef.name.contains("pet") && hydrationArgumentValue.startsWith(
            "pet", ignoreCase = true
        ) ||
            instruction.effectFieldDef.name.contains("human") && hydrationArgumentValue.startsWith(
            "human", ignoreCase = true
        )
    }
}

open class PolymorphicHydrationWithAliasTestHook : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHookUsingAliasHelper())
    }
}

@UseHook
class `batch-polymorphic-hydration` : PolymorphicHydrationWithAliasTestHook()

@UseHook
class `batch-polymorphic-hydration-actor-fields-are-in-the-same-service` : PolymorphicHydrationWithAliasTestHook()

@UseHook
class `batch-polymorphic-hydration-actor-fields-are-in-the-same-service-return-types-implement-same-interface` :
    PolymorphicHydrationWithAliasTestHook()
