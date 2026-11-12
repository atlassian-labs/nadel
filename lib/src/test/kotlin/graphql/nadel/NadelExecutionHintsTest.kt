package graphql.nadel

import graphql.nadel.hints.NadelBatchNamespacedRootFieldsHint
import graphql.nadel.test.mock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NadelExecutionHintsTest {
    private val allowedService = mock<Service>(name = "allowed")
    private val excludedService = mock<Service>(name = "excluded")

    @Test
    fun `namespaced root batching defaults to disabled globally and per service`() {
        val hints = NadelExecutionHints.newHints().build()

        assertFalse(hints.batchNamespacedRootFields())
        assertFalse(hints.batchNamespacedRootFields(allowedService))
    }

    @Test
    fun `enabling flat root batching leaves namespaced root batching disabled`() {
        val hints = NadelExecutionHints.newHints()
            .batchRootFields { true }
            .build()

        assertTrue(hints.batchRootFields())
        assertFalse(hints.batchNamespacedRootFields())
        assertFalse(hints.batchNamespacedRootFields(allowedService))
    }

    @Test
    fun `namespaced root batching defaults each service to the global flag`() {
        val hints = NadelExecutionHints.newHints()
            .batchNamespacedRootFields { true }
            .build()

        assertTrue(hints.batchNamespacedRootFields())
        assertTrue(hints.batchNamespacedRootFields(allowedService))
        assertTrue(hints.batchNamespacedRootFields(excludedService))
        assertFalse(hints.batchRootFields())
    }

    @Test
    fun `toBuilder preserves the custom namespaced root batching hint and service selection`() {
        val hint = object : NadelBatchNamespacedRootFieldsHint {
            override fun invoke(): Boolean = true

            override fun invoke(service: Service): Boolean = service === allowedService
        }
        val hints = NadelExecutionHints.newHints()
            .batchNamespacedRootFields(hint)
            .build()

        val copiedHints = hints.toBuilder()
            .batchRootFields { true }
            .build()

        assertSame(hint, hints.batchNamespacedRootFields)
        assertSame(hint, copiedHints.batchNamespacedRootFields)
        assertTrue(copiedHints.batchNamespacedRootFields())
        assertTrue(copiedHints.batchNamespacedRootFields(allowedService))
        assertFalse(copiedHints.batchNamespacedRootFields(excludedService))
        assertFalse(hints.batchRootFields())
        assertTrue(copiedHints.batchRootFields())
    }
}
