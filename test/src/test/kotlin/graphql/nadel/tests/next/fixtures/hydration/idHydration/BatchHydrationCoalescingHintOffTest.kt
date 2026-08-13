package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint

/**
 * The same fixture retains two independent Identity service calls when the rollout hint is off.
 */
class BatchHydrationCoalescingHintOffTest : BatchHydrationCoalescingDifferentSelectionsTest() {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchHydrationCoalescing(NadelBatchHydrationCoalescingHint { _ -> false })
    }
}
