package graphql.nadel.tests.next.fixtures

import graphql.nadel.NadelExecutionHints

class HydrationDeferFlagOffTest : HydrationDeferTest() {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { false }
    }
}
