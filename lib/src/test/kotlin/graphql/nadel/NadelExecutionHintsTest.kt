package graphql.nadel

import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.test.mock
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NadelExecutionHintsTest {
    @Test
    fun `batch hydration coalescing defaults off and is retained by toBuilder`() {
        val service = mock<Service>()
        val defaultHints = NadelExecutionHints.newHints().build()

        assertFalse(defaultHints.batchHydrationCoalescing(service))
        assertSame(NadelBatchHydrationCoalescingHint.disabled, defaultHints.batchHydrationCoalescing)

        val enabled = NadelBatchHydrationCoalescingHint { true }
        val enabledHints = defaultHints.toBuilder()
            .batchHydrationCoalescing(enabled)
            .build()

        assertTrue(enabledHints.batchHydrationCoalescing(service))
        assertSame(enabled, enabledHints.toBuilder().build().batchHydrationCoalescing)
    }

    @Test
    fun `generated copy retains batch hydration coalescing`() {
        val enabled = NadelBatchHydrationCoalescingHint { true }
        val enabledHints = NadelExecutionHints.newHints()
            .batchHydrationCoalescing(enabled)
            .build()

        val copy = enabledHints.copy()

        assertSame(enabled, copy.batchHydrationCoalescing)
    }
}
