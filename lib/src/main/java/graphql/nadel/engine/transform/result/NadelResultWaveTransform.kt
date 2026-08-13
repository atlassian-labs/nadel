package graphql.nadel.engine.transform.result

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.transform.result.json.NadelResultOccurrence
import graphql.nadel.engine.transform.result.json.NadelResultView
import graphql.nadel.engine.util.JsonMap
import graphql.normalized.ExecutableNormalizedField

/**
 * Internal result-transform contract for processing every invocation of one transform that is
 * ready in the current result wave.
 *
 * The public [graphql.nadel.engine.transform.NadelTransform] contract remains invocation based.
 * [NadelResultTransformer] adapts those transforms to this model, while transforms that need
 * wave-level coordination can implement this interface directly.
 */
internal interface NadelResultWaveTransform<State : Any> : NadelTransform<State> {
    suspend fun getResultInstructions(
        wave: NadelResultTransformWave<State>,
    ): NadelResultTransformOutput
}

/**
 * Inputs shared by every invocation of one transform in a result wave.
 */
internal data class NadelResultTransformContext(
    val executionContext: NadelExecutionContext,
    val serviceExecutionContext: NadelServiceExecutionContext,
    val executionBlueprint: NadelOverallExecutionBlueprint,
    val service: Service,
    val result: ServiceExecutionResult,
    val resultView: NadelResultView,
    val transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
) {
    val nodes: JsonNodes
        get() = resultView.nodes
}

/**
 * The ordered invocations of one transform that are ready in the current result wave.
 */
internal data class NadelResultTransformWave<State : Any>(
    val context: NadelResultTransformContext,
    val invocations: List<NadelResultTransformInvocation<State>>,
) {
    init {
        require(invocations.isNotEmpty()) {
            "A result transform wave must contain at least one invocation"
        }
    }
}

/**
 * One field-specific transform invocation within a result wave.
 */
internal data class NadelResultTransformInvocation<State : Any>(
    val id: NadelResultTransformInvocationId,
    val overallField: ExecutableNormalizedField,
    val underlyingParentField: ExecutableNormalizedField?,
    val state: State,
)

/**
 * Stable identity for one logical transform invocation in a result wave.
 */
@JvmInline
internal value class NadelResultTransformInvocationId(
    val ordinal: Int,
)

/**
 * Internal mutation model used while a complete result wave is being coordinated.
 *
 * Public transforms continue to return [NadelResultInstruction]. Engine transforms can also
 * retain a backing error relative to a stable occurrence until structural mutations have
 * produced the final client-shaped result.
 */
internal sealed interface NadelResultMutation {
    data class Instruction(
        val instruction: NadelResultInstruction,
    ) : NadelResultMutation

    data class AddErrorAt(
        val rawError: JsonMap,
        val subject: NadelResultOccurrence,
        val relativePath: List<Any>,
    ) : NadelResultMutation
}

/**
 * Mutations produced by a wave transform, attributed to their logical invocation.
 *
 * [NadelResultTransformer] applies entries in the original invocation order, independently of
 * this map's iteration order. A transform may omit an invocation when it has no mutations.
 */
internal class NadelResultTransformOutput private constructor(
    val mutationsByInvocationId: Map<NadelResultTransformInvocationId, List<NadelResultMutation>>,
) {
    companion object {
        fun <State : Any> forWave(
            wave: NadelResultTransformWave<State>,
            instructionsByInvocationId: Map<NadelResultTransformInvocationId, List<NadelResultInstruction>>,
        ): NadelResultTransformOutput {
            return forWaveMutations(
                wave = wave,
                mutationsByInvocationId = instructionsByInvocationId.mapValues { (_, instructions) ->
                    instructions.map(NadelResultMutation::Instruction)
                },
            )
        }

        fun <State : Any> forWaveMutations(
            wave: NadelResultTransformWave<State>,
            mutationsByInvocationId: Map<NadelResultTransformInvocationId, List<NadelResultMutation>>,
        ): NadelResultTransformOutput {
            val expectedInvocationIds = wave.invocations
                .mapTo(linkedSetOf()) { invocation ->
                    invocation.id
                }
            require(mutationsByInvocationId.keys.all { it in expectedInvocationIds }) {
                "A result wave transform returned mutations for an invocation outside " +
                    "its current result wave"
            }
            return NadelResultTransformOutput(mutationsByInvocationId)
        }
    }
}
