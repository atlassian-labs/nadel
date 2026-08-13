package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.hooks.NadelBatchHydrationCoalescingKey
import graphql.nadel.hooks.NadelExecutionHooks

/**
 * A partition hook explicitly opts into pooled inputs but violates the exact-once partition
 * contract for the pooled set. Planning therefore falls back to the ordinary isolated calls,
 * where this test hook returns valid per-consumer partitions.
 */
class BatchHydrationCoalescingCustomPartitionFallbackTest : BatchHydrationCoalescingEqualSelectionsTest() {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T> partitionBatchHydrationArgumentList(
                        argumentValues: List<T>,
                        instruction: NadelBatchHydrationFieldInstruction,
                        userContext: Any?,
                    ): List<List<T>> {
                        return if (argumentValues.size > 2) {
                            listOf(argumentValues.dropLast(1))
                        } else {
                            listOf(argumentValues)
                        }
                    }

                    override fun getBatchHydrationCoalescingKey(
                        instruction: NadelBatchHydrationFieldInstruction,
                        userContext: Any?,
                    ): NadelBatchHydrationCoalescingKey {
                        return NadelBatchHydrationCoalescingKey("invalid-pooled-partition")
                    }
                },
            )
    }
}
