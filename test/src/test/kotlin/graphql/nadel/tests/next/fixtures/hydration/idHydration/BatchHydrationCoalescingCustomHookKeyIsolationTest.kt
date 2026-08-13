package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.hooks.NadelBatchHydrationCoalescingKey
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.TestSnapshot

/**
 * Otherwise compatible consumers with different custom-hook keys remain isolated.
 */
class BatchHydrationCoalescingCustomHookKeyIsolationTest :
    BatchHydrationCoalescingEqualSelectionsTest() {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun getBatchHydrationCoalescingKey(
                        instruction: NadelBatchHydrationFieldInstruction,
                        userContext: Any?,
                    ): NadelBatchHydrationCoalescingKey {
                        return NadelBatchHydrationCoalescingKey(
                            value = instruction.location.fieldName,
                        )
                    }
                },
            )
    }

    override fun getTestSnapshot(): TestSnapshot {
        return BatchHydrationCoalescingCustomPartitionFallbackTestSnapshot()
    }
}
