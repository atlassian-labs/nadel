package graphql.nadel

import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.hints.NadelBatchRootFieldsHint
import graphql.nadel.test.mock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NadelExecutionHintsTest {
    @Test
    fun `batch hydration coalescing defaults off and is retained by toBuilder`() {
        val service = mock<Service>()
        val defaultHints = NadelExecutionHints.newHints().build()

        assertFalse(defaultHints.batchHydrationCoalescing(service))

        val enabled = NadelBatchHydrationCoalescingHint { true }
        val enabledHints = defaultHints.toBuilder()
            .batchHydrationCoalescing(enabled)
            .build()

        assertTrue(enabledHints.batchHydrationCoalescing(service))
        assertSame(enabled, enabledHints.toBuilder().build().batchHydrationCoalescing)
    }

    @Test
    fun `generated copy retains batch hydration coalescing hint`() {
        val service = mock<Service>()
        val enabled = NadelBatchHydrationCoalescingHint { true }
        val enabledHints = NadelExecutionHints.newHints()
            .batchHydrationCoalescing(enabled)
            .build()

        val copy = enabledHints.copy()

        assertTrue(copy.batchHydrationCoalescing(service))
        assertSame(enabled, copy.batchHydrationCoalescing)
    }

    @Test
    fun `batch hydration coalescing participates in value semantics`() {
        val enabled = NadelBatchHydrationCoalescingHint { true }
        val first = NadelExecutionHints.newHints()
            .batchHydrationCoalescing(enabled)
            .build()
        val second = NadelExecutionHints.newHints()
            .batchHydrationCoalescing(enabled)
            .build()
        val disabled = NadelExecutionHints.newHints().build()

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertFalse(first == disabled)
        assertTrue(first.toString().contains("batchHydrationCoalescing=$enabled"))
    }

    @Test
    fun `standalone hint is appended after existing component properties`() {
        val hintsClass = NadelExecutionHints::class.java

        assertEquals(
            NadelBatchRootFieldsHint::class.java,
            hintsClass.getDeclaredMethod("component11").returnType,
        )
        assertEquals(
            NadelBatchHydrationCoalescingHint::class.java,
            hintsClass.getDeclaredMethod("component12").returnType,
        )
    }
}
