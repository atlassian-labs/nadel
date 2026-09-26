package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

/**
 * A stable, value-based description of the selection requested by one hydration consumer.
 *
 * [ExecutableNormalizedField] does not provide the structural equality needed for grouping
 * consumers before shared hydration planning.
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
 */
internal data class NadelBatchHydrationInputConsumer(
    val stableId: Int,
    val indexKey: NadelBatchHydrationIndexKey,
    val sourceObject: JsonNode,
    val relativePath: List<Any>,
)

/**
 * Builds the deterministic, no-service-I/O part of one compatible coalescing group.
 *
 * Inputs are pooled and partitioned once. Consumers with equal selections then share a lane and
 * retain the current input deduplication behavior. Each lane batch becomes one aliased backing
 * root, and roots are packed only when their hook partition, shard and total cardinality permit.
 */
internal class NadelBatchHydrationOperationPlanner {
    private data class OperationGroupKey(
        val partitionOrdinal: Int,
        val shardingTarget: Any?,
    )

    data class SelectionLane(
        val consumers: List<NadelBatchHydrationCoalescingConsumer>,
        val aliasHelper: NadelAliasHelper,
        val batches: List<NadelSharedHydrationArgumentsBatch>,
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
        val partitionOrdinal: Int,
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
    ) {
        init {
            require(queries.isNotEmpty()) {
                "A coalesced batch hydration operation must contain a backing query"
            }
        }

        val consumers: List<NadelBatchHydrationCoalescingConsumer> = queries
            .asSequence()
            .flatMap { query -> query.contributingConsumers.asSequence() }
            .distinctBy(NadelBatchHydrationCoalescingConsumer::stableId)
            .toList()
    }

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

        val orderedConsumers = consumers.sortedBy(NadelBatchHydrationCoalescingConsumer::stableId)
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
        val operations = packBackingQueriesByExecutionBoundary(
            queries = queries,
            maxCardinality = batchSize,
        ).map(::Operation)

        return Plan(
            lanes = lanes,
            operations = operations,
        )
    }

    /**
     * Hook partitions and service shards are hard request boundaries. Inside one boundary, the
     * sum of all root batch cardinalities may not exceed the configured hydration batch size.
     */
    internal fun packBackingQueriesByExecutionBoundary(
        queries: List<BackingQuery>,
        maxCardinality: Int,
    ): List<List<BackingQuery>> {
        return queries
            .groupBy { query ->
                OperationGroupKey(
                    partitionOrdinal = query.partitionOrdinal,
                    shardingTarget = query.shardingTarget,
                )
            }
            .entries
            .sortedBy { (group) -> group.partitionOrdinal }
            .flatMap { (_, partitionQueries) ->
                packByTotalCardinality(
                    items = partitionQueries,
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
            .groupBy(NadelBatchHydrationCoalescingConsumer::selectionSignature)
            .values
            .mapIndexed { laneOrdinal, laneConsumers ->
                val representative = laneConsumers.first()
                val laneSourceInputs = getUniqueQueryableSourceInputs(laneConsumers).toSet()
                val lanePartitions = sharedPartitions.mapNotNull { partition ->
                    val sourceInputs = partition.sourceInputs.filter { sourceInput ->
                        sourceInput in laneSourceInputs
                    }
                    partition
                        .copy(sourceInputs = sourceInputs)
                        .takeIf { sourceInputs.isNotEmpty() }
                }
                val aliasHelper = NadelAliasHelper.forField(
                    tag = "batch_hydration_shared_${groupOrdinal}_$laneOrdinal",
                    field = representative.context.sourceField,
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
     * Partitions the pooled inputs once. A hook output is accepted only when its flattened values
     * are exactly the same multiset as the supplied pool; otherwise the caller falls back to
     * isolated execution.
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
            partitionedSourceInputs.hasSameValuesAs(sourceInputs)
        }
    }

    private fun getUniqueQueryableSourceInputs(
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
    ): List<JsonNode> {
        return consumers
            .asSequence()
            .flatMap { consumer -> consumer.sourceObjectsMetadata.asSequence() }
            .flatMap { metadata -> metadata.sourceInputs.orEmpty().asSequence() }
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
                    argBatches = lane.batches.map { batch -> batch.batch.arguments },
                )
                .zip(lane.batches)
                .map { (query, sharedBatch) ->
                    val batch = sharedBatch.batch
                    val rootAlias = "batch_hydration__${groupOrdinal}_${queryOrdinal++}"
                    val inputConsumers = batch.sourceInputs.flatMap { sourceInput ->
                        lane.inputConsumersBySourceInput[sourceInput].orEmpty()
                    }
                    val contributingStableIds = inputConsumers
                        .mapTo(mutableSetOf()) { inputConsumer ->
                            inputConsumer.stableId
                        }
                    val contributingConsumers = lane.consumers.filter { consumer ->
                        consumer.stableId in contributingStableIds
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
                        partitionOrdinal = sharedBatch.partitionOrdinal,
                        field = field,
                        resultPath = NadelQueryPath(
                            listOf(rootAlias) +
                                representative.instruction
                                    .queryPathToBackingField
                                    .drop(1)
                                    .segments,
                        ),
                        shardingTarget = representative.context.executionContext.hooks.getShardingTarget(
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
                                    stableId = consumer.stableId,
                                    indexKey = queryableInput.indexKey,
                                    sourceObject = sourceObject.sourceObject,
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
 * Equality is deliberately multiset based: hooks may reorder values, but may not add, remove, or
 * duplicate them when a pooled hydration is planned.
 */
internal fun List<JsonNode>.hasSameValuesAs(other: List<JsonNode>): Boolean {
    return groupingBy(JsonNode::value).eachCount() ==
        other.groupingBy(JsonNode::value).eachCount()
}

/**
 * Packs ordered items without letting their total cardinality exceed [maxCardinality].
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
