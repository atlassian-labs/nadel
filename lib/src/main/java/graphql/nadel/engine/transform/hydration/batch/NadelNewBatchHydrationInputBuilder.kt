package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationBackingFieldArgument
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder.getBatchInputDef
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder.getNonBatchInputValues
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLTypeUtil

/**
 * Represents the arguments for a hydration batch.
 *
 * There may be multiple instances of this class depending on whether
 * [NadelBatchHydrationFieldInstruction.batchSize] was exceeded etc.
 */
internal data class NadelHydrationArgumentsBatch(
    val sourceInputs: List<JsonNode>,
    val arguments: Map<NadelHydrationBackingFieldArgument, NormalizedInputValue>,
)

/**
 * An [NormalizedInputValue] for one query to a service.
 *
 * i.e. this object represents one batch of the [sourceInputs] values that we send down.
 *
 * An intermediary object to store info while we pass data around functions.
 */
private data class BatchedArgumentValue(
    val sourceInputs: List<JsonNode>,
    val argumentDef: NadelHydrationBackingFieldArgument,
    val argumentValue: NormalizedInputValue,
)

/**
 * todo: does this apply even with the new matcher?
 *
 * README
 *
 * Please ensure that the batch arguments are ordered according to the input.
 * This is required for [NadelBatchHydrationMatchStrategy.MatchIndex].
 */
internal object NadelNewBatchHydrationInputBuilder {
    fun getInputValueBatches(
        hooks: NadelExecutionHooks,
        userContext: Any?,
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: ExecutableNormalizedField,
        sourceInputs: List<JsonNode>,
    ): List<NadelHydrationArgumentsBatch> {
        val nonBatchArgs = getNonBatchInputValues(instruction, hydrationField)
        val batchArgs = getBatchArgumentValue(instruction, sourceInputs, hooks, userContext)

        return batchArgs
            .map { batchedArgument ->
                NadelHydrationArgumentsBatch(
                    arguments = nonBatchArgs + (batchedArgument.argumentDef to batchedArgument.argumentValue),
                    sourceInputs = batchedArgument.sourceInputs,
                )
            }
    }

    private fun getBatchArgumentValue(
        instruction: NadelBatchHydrationFieldInstruction,
        sourceInputs: List<JsonNode>,
        hooks: NadelExecutionHooks,
        userContext: Any?,
    ): List<BatchedArgumentValue> {
        val batchSize = instruction.batchSize

        val (batchInputDef) = getBatchInputDef(instruction) ?: return emptyList()
        val actorBatchArgDef = instruction.backingFieldDef.getArgument(batchInputDef.name)

        val partitionArgumentList = hooks.partitionBatchHydrationArgumentList(
            argumentValues = sourceInputs.map { it.value },
            instruction = instruction,
            userContext = userContext,
        )

        return partitionArgumentList
            .flatMap {
                it.chunked(size = batchSize)
            }
            .map { chunk ->
                val normalizedInputValue = NormalizedInputValue(
                    GraphQLTypeUtil.simplePrint(actorBatchArgDef.type),
                    javaValueToAstValue(chunk),
                )

                BatchedArgumentValue(
                    sourceInputs = chunk
                        .map {
                            JsonNode(it)
                        },
                    argumentDef = batchInputDef,
                    argumentValue = normalizedInputValue,
                )
            }
    }
}
