package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.test.mock
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertNull

class NadelBatchHydrationCoalescingConsumerTest {
    @Test
    fun `indexed hydration cannot become a coalescing consumer`() {
        val instruction = mock<NadelBatchHydrationFieldInstruction> { instruction ->
            every { instruction.batchHydrationMatchStrategy } returns
                NadelBatchHydrationMatchStrategy.MatchIndex
        }

        val consumer = NadelBatchHydrationCoalescingConsumer.createOrNull(
            stableId = 0,
            hydration = mock(),
            instruction = instruction,
        )

        assertNull(consumer)
    }
}
