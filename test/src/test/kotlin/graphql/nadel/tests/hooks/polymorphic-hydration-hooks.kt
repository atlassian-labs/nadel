package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodePath
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.UseHook

private class PolymorphicHydrationHooks : NadelEngineExecutionHooks {
    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
        instructions: List<T>,
        parentNode: JsonNode
    ): T {

        val dataIdFieldName = if (instructions.any { it is NadelHydrationFieldInstruction })
            "hydration__data__dataId"
        else "batch_hydration__data__dataId"

        val dataIdValue = JsonNodeExtractor.getNodeAt(parentNode, JsonNodePath.root + dataIdFieldName)!!
            .value as String
        val actorFieldName = when {
            dataIdValue.startsWith("human", ignoreCase = true) -> "humanById"
            else -> "petById"
        }
        return instructions.single { it.actorFieldDef.name == actorFieldName }
    }

    override fun <T> partitionArgumentList(args: List<T>): List<List<T>> {
        return listOf(args)
    }
}

@UseHook
class `solitary-polymorphic-hydration` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHooks())
    }
}

@UseHook
class `batch-polymorphic-hydration` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHooks())
    }
}

@UseHook
class `batch-polymorphic-hydration-with-interfaces` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHooks())
    }
}

@UseHook
class `batch-polymorphic-hydration-with-unions` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHooks())
    }
}

@UseHook
class `batch-polymorphic-hydration-with-rename` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHooks())
    }
}

@UseHook
class `batch-polymorphic-hydration-where-only-one-type-is-queried` : EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder.serviceExecutionHooks(PolymorphicHydrationHooks())
    }
}
