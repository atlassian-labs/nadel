package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private class PolymorphicHydrationHookUsingAliasHelper : NadelExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
        userContext: Any?,
    ): T? {
        return instructions.firstOrNull {
            val (_, _, valueSource) = it.actorInputValueDefs.single()
            if (valueSource !is NadelHydrationActorInputDef.ValueSource.FieldResultValue) {
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
        return instruction.actorFieldDef.name.contains("pet") && hydrationArgumentValue.startsWith(
            "pet", ignoreCase = true
        ) ||
            instruction.actorFieldDef.name.contains("human") && hydrationArgumentValue.startsWith(
            "human", ignoreCase = true
        )
    }
}

open class PolymorphicHydrationWithAliasTestHook : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(PolymorphicHydrationHookUsingAliasHelper())
    }
}

@UseHook
class `batch-polymorphic-hydration` : PolymorphicHydrationWithAliasTestHook()

@UseHook
class `batch-polymorphic-hydration-actor-fields-are-in-the-same-service` : PolymorphicHydrationWithAliasTestHook()

@UseHook
class `batch-polymorphic-hydration-actor-fields-are-in-the-same-service-return-types-implement-same-interface` :
    PolymorphicHydrationWithAliasTestHook()
