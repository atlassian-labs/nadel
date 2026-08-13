package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder.getBatchInputDef
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder.getNonBatchInputValues
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLTypeUtil

/**
 * One equality-preserving partition returned by
 * [NadelExecutionHooks.partitionBatchHydrationArgumentList].
 */
internal data class NadelBatchHydrationArgumentPartition(
    val ordinal: Int,
    val sourceInputs: List<JsonNode>,
)

/**
 * Represents the arguments for one hydration query.
 *
 * There may be multiple instances of this class depending on whether
 * [NadelBatchHydrationFieldInstruction.batchSize] was exceeded. [partitionOrdinal] keeps the
 * hook boundary that the query came from so a planner never combines distinct partitions into
 * one GraphQL operation.
 */
internal data class NadelHydrationArgumentsBatch(
    val partitionOrdinal: Int,
    val sourceInputs: List<JsonNode>,
    val arguments: Map<NadelHydrationArgument, NormalizedInputValue>,
)

/**
 * An [NormalizedInputValue] for one query to a service.
 *
 * i.e. this object represents one batch of the [sourceInputs] values that we send down.
 *
 * An intermediary object to store info while we pass data around functions.
 */
private data class BatchedArgumentValue(
    val partitionOrdinal: Int,
    val sourceInputs: List<JsonNode>,
    val argumentDef: NadelHydrationArgument,
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
        val partitions = getInputPartitions(
            hooks = hooks,
            userContext = userContext,
            instruction = instruction,
            sourceInputs = sourceInputs,
        )
        return getInputValueBatches(
            instruction = instruction,
            hydrationField = hydrationField,
            partitions = partitions,
        )
    }

    fun getInputPartitions(
        hooks: NadelExecutionHooks,
        userContext: Any?,
        instruction: NadelBatchHydrationFieldInstruction,
        sourceInputs: List<JsonNode>,
    ): List<NadelBatchHydrationArgumentPartition> {
        return hooks.partitionBatchHydrationArgumentList(
            argumentValues = sourceInputs.map(JsonNode::value),
            instruction = instruction,
            userContext = userContext,
        ).mapIndexed { ordinal, partition ->
            NadelBatchHydrationArgumentPartition(
                ordinal = ordinal,
                sourceInputs = partition.map(::JsonNode),
            )
        }
    }

    fun getInputValueBatches(
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: ExecutableNormalizedField,
        partitions: List<NadelBatchHydrationArgumentPartition>,
    ): List<NadelHydrationArgumentsBatch> {
        val nonBatchArgs = getNonBatchInputValues(instruction, hydrationField)
        val batchArgs = getBatchArgumentValue(
            instruction = instruction,
            partitions = partitions,
        )

        return batchArgs
            .map { batchedArgument ->
                NadelHydrationArgumentsBatch(
                    partitionOrdinal = batchedArgument.partitionOrdinal,
                    arguments = nonBatchArgs + (batchedArgument.argumentDef to batchedArgument.argumentValue),
                    sourceInputs = batchedArgument.sourceInputs,
                )
            }
    }

    private fun getBatchArgumentValue(
        instruction: NadelBatchHydrationFieldInstruction,
        partitions: List<NadelBatchHydrationArgumentPartition>,
    ): List<BatchedArgumentValue> {
        val batchSize = instruction.batchSize

        val (batchInputDef) = getBatchInputDef(instruction) ?: return emptyList()
        val batchArgDef = instruction.backingFieldDef.getArgument(batchInputDef.name)

        return partitions.flatMap { partition ->
            partition.sourceInputs
                .chunked(size = batchSize)
                .map { chunk ->
                    val normalizedInputValue = NormalizedInputValue(
                        GraphQLTypeUtil.simplePrint(batchArgDef.type),
                        javaValueToAstValue(chunk.map(JsonNode::value)),
                    )

                    BatchedArgumentValue(
                        partitionOrdinal = partition.ordinal,
                        sourceInputs = chunk,
                        argumentDef = batchInputDef,
                        argumentValue = normalizedInputValue,
                    )
                }
            }
    }
}
