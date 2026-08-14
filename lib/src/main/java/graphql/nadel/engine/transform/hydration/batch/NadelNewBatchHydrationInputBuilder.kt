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
 * Represents the arguments for a hydration batch.
 *
 * There may be multiple instances of this class depending on whether
 * [NadelBatchHydrationFieldInstruction.batchSize] was exceeded etc.
 */
internal data class NadelHydrationArgumentsBatch(
    val sourceInputs: List<JsonNode>,
    val arguments: Map<NadelHydrationArgument, NormalizedInputValue>,
)

/**
 * Shared-planner metadata that must not burden the ordinary hydration path.
 */
internal data class NadelSharedHydrationArgumentsBatch(
    val partitionOrdinal: Int,
    val batch: NadelHydrationArgumentsBatch,
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
    val argumentDef: NadelHydrationArgument,
    val argumentValue: NormalizedInputValue,
)

private data class NadelPartitionedBatchedArgumentValue(
    val partitionOrdinal: Int,
    val value: BatchedArgumentValue,
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
    ): List<NadelSharedHydrationArgumentsBatch> {
        val nonBatchArgs = getNonBatchInputValues(instruction, hydrationField)
        val batchArgs = getBatchArgumentValueForPartitions(
            instruction = instruction,
            partitions = partitions,
        )

        return batchArgs
            .map { batchedArgument ->
                NadelSharedHydrationArgumentsBatch(
                    partitionOrdinal = batchedArgument.partitionOrdinal,
                    batch = NadelHydrationArgumentsBatch(
                        arguments = nonBatchArgs +
                            (batchedArgument.value.argumentDef to batchedArgument.value.argumentValue),
                        sourceInputs = batchedArgument.value.sourceInputs,
                    ),
                )
            }
    }

    private fun getBatchArgumentValueForPartitions(
        instruction: NadelBatchHydrationFieldInstruction,
        partitions: List<NadelBatchHydrationArgumentPartition>,
    ): List<NadelPartitionedBatchedArgumentValue> {
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

                    NadelPartitionedBatchedArgumentValue(
                        partitionOrdinal = partition.ordinal,
                        value = BatchedArgumentValue(
                            sourceInputs = chunk,
                            argumentDef = batchInputDef,
                            argumentValue = normalizedInputValue,
                        ),
                    )
                }
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
        val batchArgDef = instruction.backingFieldDef.getArgument(batchInputDef.name)

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
                    GraphQLTypeUtil.simplePrint(batchArgDef.type),
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
