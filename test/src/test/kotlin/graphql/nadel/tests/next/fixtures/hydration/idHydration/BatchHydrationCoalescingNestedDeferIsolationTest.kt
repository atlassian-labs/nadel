package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalExecutionResult
import graphql.nadel.NadelExecutionHints
import kotlin.test.assertTrue

/**
 * Coalescing must not move a nested deferred hydration into the initial delivery.
 *
 * Although both fields use the same type-level default and selection, the Identity snapshot
 * should retain one initial call for `assignee` and one deferred call for `reporter`.
 */
class BatchHydrationCoalescingNestedDeferIsolationTest : BatchHydrationCoalescingEqualSelectionsTest() {
    override fun makeExecutionInput() = super.makeExecutionInput()
        .query(
            """
                query {
                  issues {
                    assignee {
                      name
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
