package graphql.nadel

import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.hints.NadelBatchRootFieldsHint
import graphql.nadel.test.mock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
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
    fun `generated copy retains its existing ABI and falls back to disabled coalescing`() {
        val service = mock<Service>()
        val enabled = NadelBatchHydrationCoalescingHint { true }
        val enabledHints = NadelExecutionHints.newHints()
            .batchHydrationCoalescing(enabled)
            .build()

        val copy = enabledHints.copy()

        assertFalse(copy.batchHydrationCoalescing(service))
        assertSame(NadelBatchHydrationCoalescingHint.disabled, copy.batchHydrationCoalescing)
    }

    @Test
    fun `batch hydration coalescing does not change data class value semantics`() {
        val enabled = NadelBatchHydrationCoalescingHint { true }
        val enabledHints = NadelExecutionHints.newHints()
            .batchHydrationCoalescing(enabled)
            .build()
        val disabled = NadelExecutionHints.newHints().build()

        assertEquals(enabledHints, disabled)
        assertEquals(enabledHints.hashCode(), disabled.hashCode())
        assertFalse(enabledHints.toString().contains("batchHydrationCoalescing"))
    }

    @Test
    fun `standalone hint does not alter existing component or copy ABI`() {
        val hintsClass = NadelExecutionHints::class.java

        assertEquals(
            NadelBatchRootFieldsHint::class.java,
            hintsClass.getDeclaredMethod("component11").returnType,
        )
        assertFailsWith<NoSuchMethodException> {
            hintsClass.getDeclaredMethod("component12")
        }
        assertEquals(
            11,
            hintsClass.getDeclaredMethods().single { it.name == "copy" }.parameterCount,
        )
    }
}
