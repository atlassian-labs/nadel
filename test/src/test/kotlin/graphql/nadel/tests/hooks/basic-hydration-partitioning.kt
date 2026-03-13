package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private class HydrationPartitioningHooks : NadelExecutionHooks {
    override fun <T> partitionBatchHydrationArgumentList(
        argumentValues: List<T>,
        instruction: NadelBatchHydrationFieldInstruction,
        userContext: Any?,
    ): List<List<T>> {
        // Only partition when pathToPartitionArg is set
        if (instruction.pathToPartitionArg == null) {
            return listOf(argumentValues)
        }
        // Group by the prefix before "/" e.g. "CLOUD-1/USER-1" -> "CLOUD-1"
        return argumentValues
            .groupBy { value ->
                (value as String).substringBefore("/")
            }
            .values
            .toList()
    }
}

@UseHook
class `basic-hydration-partitioning` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.executionHooks(HydrationPartitioningHooks())
    }
}
