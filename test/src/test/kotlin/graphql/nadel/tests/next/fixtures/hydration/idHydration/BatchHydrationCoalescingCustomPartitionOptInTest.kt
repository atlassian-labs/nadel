package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.Nadel
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.hooks.NadelExecutionHooks
import kotlin.test.assertEquals

/**
 * With coalescing enabled, a custom partition hook sees the pooled inputs once while preserving
 * its partition boundaries. Each returned partition remains a separate backing request.
 */
class BatchHydrationCoalescingCustomPartitionOptInTest : BatchHydrationCoalescingEqualSelectionsTest() {
    private val partitionCalls = mutableListOf<List<Any?>>()

    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T> partitionBatchHydrationArgumentList(
                        argumentValues: List<T>,
                        instruction: NadelBatchHydrationFieldInstruction,
                        userContext: Any?,
                    ): List<List<T>> {
                        partitionCalls += argumentValues
                        return listOf(
                            argumentValues.take(2),
                            argumentValues.drop(2),
                        )
                    }
                },
            )
    }

    override fun assert(
        result: ExecutionResult,
        incrementalResults: List<DelayedIncrementalPartialResult>?,
    ) {
        assertEquals<List<List<Any?>>>(
            expected = listOf(
                listOf(
                    "ari:cloud:identity::user/1",
                    "ari:cloud:identity::user/2",
                    "ari:cloud:identity::user/3",
                ),
            ),
            actual = partitionCalls,
        )
    }
}
