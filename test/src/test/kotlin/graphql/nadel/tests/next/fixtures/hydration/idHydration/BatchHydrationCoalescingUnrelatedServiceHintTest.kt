package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint

/**
 * Enabling coalescing for the source service must not enable it for the hydration service.
 *
 * The Identity snapshot should retain the two independent hydration requests inherited from
 * [BatchHydrationCoalescingEqualSelectionsTest].
 */
class BatchHydrationCoalescingUnrelatedServiceHintTest : BatchHydrationCoalescingEqualSelectionsTest() {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchHydrationCoalescing(
                NadelBatchHydrationCoalescingHint { service ->
                    service.name == "Jira"
                },
            )
    }
}
