package graphql.nadel.engine.transform.result

import graphql.nadel.test.mock
import graphql.normalized.ExecutableNormalizedField
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NadelResultTransformWaveTest {
    @Test
    fun `output accepts current wave invocation ids in any map order`() {
        val firstId = NadelResultTransformInvocationId(0)
        val secondId = NadelResultTransformInvocationId(1)
        val wave = makeWave(listOf(firstId, secondId))
        val instructionsByInvocationId =
            linkedMapOf<NadelResultTransformInvocationId, List<NadelResultInstruction>>(
                secondId to emptyList(),
                firstId to emptyList(),
            )

        val output = NadelResultTransformOutput.forWave(
            wave = wave,
            instructionsByInvocationId = instructionsByInvocationId,
        )

        assertEquals(
            expected = instructionsByInvocationId.mapValues { (_, instructions) ->
                instructions.map(NadelResultMutation::Instruction)
            },
            actual = output.mutationsByInvocationId,
        )
    }

    @Test
    fun `output rejects an invocation outside its wave`() {
        val wave = makeWave(listOf(NadelResultTransformInvocationId(0)))

        assertFailsWith<IllegalArgumentException> {
            NadelResultTransformOutput.forWave(
                wave = wave,
                instructionsByInvocationId = mapOf(
                    NadelResultTransformInvocationId(1) to emptyList(),
                ),
            )
        }
    }

    private fun makeWave(
        invocationIds: List<NadelResultTransformInvocationId>,
    ): NadelResultTransformWave<Unit> {
        return NadelResultTransformWave(
            context = mock(relaxed = true),
            invocations = invocationIds.map { invocationId ->
                NadelResultTransformInvocation(
                    id = invocationId,
                    overallField = mock<ExecutableNormalizedField>(),
                    underlyingParentField = null,
                    state = Unit,
                )
            },
        )
    }
}
