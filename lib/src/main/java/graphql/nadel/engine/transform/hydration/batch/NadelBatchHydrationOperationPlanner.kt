package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.NadelResultOccurrence
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

/**
 * A stable, value-based description of the selection requested by one hydration consumer.
 *
 * [ExecutableNormalizedField] does not provide the structural equality needed for grouping
 * consumers into shared selection lanes.
 */
internal data class NadelBatchHydrationSelectionSignature(
    val fields: List<Field>,
) {
    internal data class Field(
        val fieldName: String,
        val resultKey: String,
        val normalizedArguments: Map<String, NormalizedInputValue>,
        val objectTypeNames: Set<String>,
        val children: List<Field>,
    )

    companion object {
        fun from(fields: List<ExecutableNormalizedField>): NadelBatchHydrationSelectionSignature {
            fun makeField(field: ExecutableNormalizedField): Field {
                return Field(
                    fieldName = field.name,
                    resultKey = field.resultKey,
                    normalizedArguments = field.normalizedArguments,
                    objectTypeNames = field.objectTypeNames.toSet(),
                    children = field.children.map(::makeField),
                )
            }

            return NadelBatchHydrationSelectionSignature(
                fields = fields.map(::makeField),
            )
        }
    }
}

/**
 * One client result location that consumed a deduplicated backing input.
 *
 * Recording this while the operation is planned avoids reconstructing provenance from source
 * objects after the backing request has completed.
 */
internal data class NadelBatchHydrationInputConsumer(
    val invocationId: Int,
    val indexKey: NadelBatchHydrationIndexKey,
    val sourceOccurrence: NadelResultOccurrence,
    val relativePath: List<Any>,
)

/**
 * Builds the deterministic, no-service-I/O part of a coalesced hydration group.
 *
 * The resulting model makes each transition explicit:
 *
 * 1. Consumers with the same effective selection form a [SelectionLane].
 * 2. Each argument batch in a lane becomes one aliased [BackingQuery].
 * 3. Consecutive queries are packed into bounded [Operation]s.
 *
 * Every operation carries its exact consumers, so execution, metadata and error attribution all
 * use the same provenance instead of reconstructing it independently.
 */
internal class NadelBatchHydrationOperationPlanner {
    private data class OperationGroupKey(
        val partitionOrdinal: Int,
        val shardingTarget: Any?,
    )

    data class SelectionLane(
        val consumers: List<NadelBatchHydrationCoalescingConsumer>,
        val aliasHelper: NadelAliasHelper,
        val batches: List<NadelHydrationArgumentsBatch>,
        val inputConsumersBySourceInput: Map<JsonNode, List<NadelBatchHydrationInputConsumer>>,
    ) {
        val objectIdentifiers: List<NadelBatchHydrationMatchStrategy.MatchObjectIdentifier>
            get() = consumers.first().objectIdentifiers
    }

    data class BackingQuery(
        val lane: SelectionLane,
        val contributingConsumers: List<NadelBatchHydrationCoalescingConsumer>,
        val inputConsumers: List<NadelBatchHydrationInputConsumer>,
        val batch: NadelHydrationArgumentsBatch,
        val field: ExecutableNormalizedField,
        val resultPath: NadelQueryPath,
        val shardingTarget: Any?,
    ) {
        val inputConsumersByIndexKey: Map<NadelBatchHydrationIndexKey, List<NadelBatchHydrationInputConsumer>> =
            inputConsumers.groupBy { inputConsumer ->
                inputConsumer.indexKey
            }
    }

    data class Operation(
        val queries: List<BackingQuery>,
        val consumers: List<NadelBatchHydrationCoalescingConsumer>,
    )

    data class Plan(
        val lanes: List<SelectionLane>,
        val operations: List<Operation>,
    )

    fun plan(
        groupOrdinal: Int,
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
    ): Plan? {
        require(consumers.isNotEmpty()) {
            "A shared batch hydration group must contain at least one consumer"
        }

        val orderedConsumers = consumers.sortedWith(
            compareBy<NadelBatchHydrationCoalescingConsumer>(
                { it.context.sourceField.listOfResultKeys.joinToString(separator = "\u0000") },
                { it.invocation.id },
            ),
        )
        val sharedPartitions = getSharedInputPartitions(orderedConsumers)
            ?: return null
        val lanes = makeSelectionLanes(
            groupOrdinal = groupOrdinal,
            consumers = orderedConsumers,
            sharedPartitions = sharedPartitions,
        )
        val queries = makeBackingQueries(
            groupOrdinal = groupOrdinal,
            lanes = lanes,
        )
        val batchSize = orderedConsumers.first().instruction.batchSize
        val packedQueries = packBackingQueriesByExecutionBoundary(
            queries = queries,
            maxCardinality = batchSize,
        )
        val operations = packedQueries.map { operationQueries ->
            val operationConsumers = operationQueries
                .asSequence()
                .flatMap { query -> query.contributingConsumers.asSequence() }
                .distinctBy { consumer -> consumer.invocation.id }
                .toList()

            check(operationConsumers.isNotEmpty()) {
                "Every shared backing operation must have at least one contributing consumer"
            }

            Operation(
                queries = operationQueries,
                consumers = operationConsumers,
            )
        }

        return Plan(
            lanes = lanes,
            operations = operations,
        )
    }

    /**
     * Keeps hook partitions and service shards as hard request boundaries, then applies the
     * total operation-size bound independently inside each boundary.
     */
    internal fun packBackingQueriesByExecutionBoundary(
        queries: List<BackingQuery>,
        maxCardinality: Int,
    ): List<List<BackingQuery>> {
        return queries
            .groupBy { query ->
                OperationGroupKey(
                    partitionOrdinal = query.batch.partitionOrdinal,
                    shardingTarget = query.shardingTarget,
                )
            }
            .entries
            .sortedBy { (group) -> group.partitionOrdinal }
            .flatMap { partitionQueries ->
                packByTotalCardinality(
                    items = partitionQueries.value,
                    maxCardinality = maxCardinality,
                    cardinality = { query -> query.batch.sourceInputs.size },
                )
            }
    }

    private fun makeSelectionLanes(
        groupOrdinal: Int,
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
        sharedPartitions: List<NadelBatchHydrationArgumentPartition>,
    ): List<SelectionLane> {
        return consumers
            .groupBy { consumer -> consumer.selectionSignature }
            .values
            .mapIndexed { laneOrdinal, laneConsumers ->
                val representative = laneConsumers.first()
                val uniqueSourceInputs = getUniqueQueryableSourceInputs(laneConsumers)
                val laneSourceInputs = uniqueSourceInputs.toSet()
                val lanePartitions = sharedPartitions.mapNotNull { partition ->
                    val partitionSourceInputs = partition.sourceInputs.filter { sourceInput ->
                        sourceInput in laneSourceInputs
                    }
                    if (partitionSourceInputs.isEmpty()) {
                        null
                    } else {
                        partition.copy(sourceInputs = partitionSourceInputs)
                    }
                }
                val aliasHelper = NadelAliasHelper.forField(
                    tag = "batch_hydration_shared_${groupOrdinal}_$laneOrdinal",
                    field = representative.invocation.state.virtualField,
                )
                val batches = NadelNewBatchHydrationInputBuilder.getInputValueBatches(
                    instruction = representative.instruction,
                    hydrationField = representative.context.sourceField,
                    partitions = lanePartitions,
                )

                SelectionLane(
                    consumers = laneConsumers,
                    aliasHelper = aliasHelper,
                    batches = batches,
                    inputConsumersBySourceInput = getInputConsumersBySourceInput(laneConsumers),
                )
            }
    }

    /**
     * Partitions the pooled inputs once so every selection lane observes the same hook boundary.
     *
     * A custom hook that replaces, drops or duplicates values cannot be safely pooled with the
     * current hook API because its output has no source identity. Returning null keeps those
     * consumers on the existing isolated execution path.
     */
    private fun getSharedInputPartitions(
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
    ): List<NadelBatchHydrationArgumentPartition>? {
        val representative = consumers.first()
        val sourceInputs = getUniqueQueryableSourceInputs(consumers)
        val partitions = NadelNewBatchHydrationInputBuilder.getInputPartitions(
            hooks = representative.context.executionContext.hooks,
            userContext = representative.context.executionContext.userContext,
            instruction = representative.instruction,
            sourceInputs = sourceInputs,
        )
        val partitionedSourceInputs = partitions.flatMap { partition ->
            partition.sourceInputs
        }

        return partitions.takeIf {
            partitionedSourceInputs.size == sourceInputs.size &&
                partitionedSourceInputs.toSet() == sourceInputs.toSet()
        }
    }

    private fun getUniqueQueryableSourceInputs(
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
    ): List<JsonNode> {
        return consumers
            .asSequence()
            .flatMap { consumer -> consumer.sourceInputs.asSequence() }
            .filterIsInstance<NadelNewBatchHydrator.SourceInput.Queryable>()
            .map { sourceInput -> sourceInput.sourceInputNode }
            .filter { sourceInput -> sourceInput.value != null }
            .toCollection(LinkedHashSet<JsonNode>())
            .toList()
    }

    private fun makeBackingQueries(
        groupOrdinal: Int,
        lanes: List<SelectionLane>,
    ): List<BackingQuery> {
        var queryOrdinal = 0

        return lanes.flatMap { lane ->
            val representative = lane.consumers.first()
            NadelHydrationFieldsBuilder
                .makeBatchBackingQueries(
                    executionHints = representative.context.executionContext.hints,
                    executionBlueprint = representative.context.executionBlueprint,
                    instruction = representative.instruction,
                    aliasHelper = lane.aliasHelper,
                    virtualField = representative.context.sourceField,
                    argBatches = lane.batches.map { batch -> batch.arguments },
                )
                .zip(lane.batches)
                .map { (query, batch) ->
                    val rootAlias = "batch_hydration__${groupOrdinal}_${queryOrdinal++}"
                    val inputConsumers = batch.sourceInputs.flatMap { sourceInput ->
                        lane.inputConsumersBySourceInput[sourceInput].orEmpty()
                    }
                    val contributingInvocationIds = inputConsumers
                        .mapTo(mutableSetOf()) { inputConsumer ->
                            inputConsumer.invocationId
                        }
                    val contributingConsumers = lane.consumers.filter { consumer ->
                        consumer.invocation.id in contributingInvocationIds
                    }
                    check(contributingConsumers.isNotEmpty()) {
                        "Every shared backing query must have at least one contributing consumer"
                    }

                    val field = query.toBuilder()
                        .alias(rootAlias)
                        .build()
                    BackingQuery(
                        lane = lane,
                        contributingConsumers = contributingConsumers,
                        inputConsumers = inputConsumers,
                        batch = batch,
                        field = field,
                        resultPath = NadelQueryPath(
                            listOf(rootAlias) +
                                representative.instruction
                                    .queryPathToBackingField
                                    .drop(1)
                                    .segments,
                        ),
                        shardingTarget =
                            representative.context.executionContext.hooks.getShardingTarget(
                                executionContext = representative.context.executionContext,
                                service = representative.instruction.backingService,
                                field = field,
                            ),
                    )
                }
        }
    }

    private fun getInputConsumersBySourceInput(
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
    ): Map<JsonNode, List<NadelBatchHydrationInputConsumer>> {
        val inputConsumersBySourceInput =
            LinkedHashMap<JsonNode, MutableList<NadelBatchHydrationInputConsumer>>()

        consumers.forEach { consumer ->
            consumer.sourceObjectsMetadata.forEach { sourceObject ->
                sourceObject.sourceInputs
                    .orEmpty()
                    .forEachIndexed { sourceInputIndex, sourceInput ->
                        val queryableInput =
                            sourceInput as? NadelNewBatchHydrator.SourceInput.Queryable
                                ?: return@forEachIndexed

                        val relativePath = buildList<Any> {
                            add(consumer.context.sourceField.resultKey)
                            if (consumer.context.isSourceFieldListOutput) {
                                add(sourceInputIndex)
                            }
                        }

                        inputConsumersBySourceInput
                            .getOrPut(queryableInput.sourceInputNode, ::mutableListOf)
                            .add(
                                NadelBatchHydrationInputConsumer(
                                    invocationId = consumer.invocation.id,
                                    indexKey = queryableInput.indexKey,
                                    sourceOccurrence = sourceObject.sourceOccurrence,
                                    relativePath = relativePath,
                                ),
                            )
                    }
            }
        }

        return inputConsumersBySourceInput
    }
}

/**
 * Packs ordered items without allowing the sum of their cardinalities to exceed
 * [maxCardinality]. The input order is preserved.
 */
internal fun <T> packByTotalCardinality(
    items: List<T>,
    maxCardinality: Int,
    cardinality: (T) -> Int,
): List<List<T>> {
    require(maxCardinality > 0) {
        "Maximum cardinality must be greater than zero"
    }

    return buildList {
        var operationItems = mutableListOf<T>()
        var operationCardinality = 0

        items.forEach { item ->
            val itemCardinality = cardinality(item)
            require(itemCardinality in 1..maxCardinality) {
                "Each item must have a cardinality between 1 and $maxCardinality"
            }

            if (operationItems.isNotEmpty() &&
                operationCardinality > maxCardinality - itemCardinality
            ) {
                add(operationItems)
                operationItems = mutableListOf()
                operationCardinality = 0
            }

            operationItems += item
            operationCardinality += itemCardinality
        }

        if (operationItems.isNotEmpty()) {
            add(operationItems)
        }
    }
}
