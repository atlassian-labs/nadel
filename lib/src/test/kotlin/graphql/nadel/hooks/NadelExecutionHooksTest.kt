package graphql.nadel.hooks

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.test.mock
import graphql.normalized.ExecutableNormalizedField
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NadelExecutionHooksTest {
    private val instruction = mock<NadelBatchHydrationFieldInstruction>()
    private val userContext = Any()

    @Test
    fun `built-in hooks allow batch hydration coalescing`() {
        val firstKey = assertNotNull(
            NadelExecutionHooks.default.getBatchHydrationCoalescingKey(
                instruction = instruction,
                userContext = userContext,
            ),
        )
        val secondKey = assertNotNull(
            NadelExecutionHooks.default.getBatchHydrationCoalescingKey(
                instruction = instruction,
                userContext = userContext,
            ),
        )

        assertEquals(firstKey, secondKey)
    }

    @Test
    fun `custom hydration instruction hook does not allow coalescing by default`() {
        val hooks = object : NadelExecutionHooks {
            override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                virtualField: ExecutableNormalizedField,
                instructions: List<T>,
                parentNode: JsonNode,
                aliasHelper: NadelAliasHelper,
                userContext: Any?,
            ): T? {
                return instructions.single()
            }
        }

        assertNull(
            hooks.getBatchHydrationCoalescingKey(
                instruction = instruction,
                userContext = userContext,
            ),
        )
    }

    @Test
    fun `custom argument partition hook does not allow coalescing by default`() {
        val hooks = object : NadelExecutionHooks {
            override fun <T> partitionBatchHydrationArgumentList(
                argumentValues: List<T>,
                instruction: NadelBatchHydrationFieldInstruction,
                userContext: Any?,
            ): List<List<T>> {
                return listOf(argumentValues)
            }
        }

        assertNull(
            hooks.getBatchHydrationCoalescingKey(
                instruction = instruction,
                userContext = userContext,
            ),
        )
    }

    @Test
    fun `custom hooks can explicitly choose a coalescing group`() {
        val expectedKey = NadelBatchHydrationCoalescingKey("partition-v1")
        val hooks = object : NadelExecutionHooks {
            override fun getBatchHydrationCoalescingKey(
                instruction: NadelBatchHydrationFieldInstruction,
                userContext: Any?,
            ): NadelBatchHydrationCoalescingKey {
                return expectedKey
            }
        }

        assertEquals(
            expected = expectedKey,
            actual = hooks.getBatchHydrationCoalescingKey(
                instruction = instruction,
                userContext = userContext,
            ),
        )
    }

    @Test
    fun `coalescing keys use their wrapped value for compatibility`() {
        assertEquals(
            NadelBatchHydrationCoalescingKey("same"),
            NadelBatchHydrationCoalescingKey("same"),
        )
        assertNotEquals(
            NadelBatchHydrationCoalescingKey("first"),
            NadelBatchHydrationCoalescingKey("second"),
        )
    }
}
