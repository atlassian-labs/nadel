package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalExecutionResult
import graphql.nadel.NadelExecutionHints
import kotlin.test.assertTrue

/**
 * Two deferred consumers remain isolated in Phase 1 even though they have equal selections and
 * the same type-level default. The Identity snapshot should contain two independent hydration
 * calls, and both values should remain in incremental delivery.
 */
class BatchHydrationCoalescingTwoDeferredConsumersIsolationTest :
    BatchHydrationCoalescingEqualSelectionsTest() {
    override fun makeExecutionInput() = super.makeExecutionInput()
        .query(
            """
                query {
                  issues {
                    ... @defer(label: "assignee") {
                      assignee {
                        name
                      }
                    }
                    ... @defer(label: "reporter") {
                      reporter {
                        name
                      }
                    }
                  }
                }
            """.trimIndent(),
        )

    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }

    override fun assert(
        result: ExecutionResult,
        incrementalResults: List<DelayedIncrementalPartialResult>?,
    ) {
        assertTrue(result is IncrementalExecutionResult)
        assertTrue(incrementalResults?.isNotEmpty() == true)
    }
}
