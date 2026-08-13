package graphql.nadel.engine.transform.hydration.batch

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument.ValueSource
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexBasedIndexer
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexer
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationObjectIdentifiedIndexer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.NadelResultOccurrence
import graphql.nadel.engine.util.deepCopyJsonValue
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.getField
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.zipOrThrow
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Prepares and executes batch hydrations.
 *
 * Wave-level coalescing is delegated to [NadelBatchHydrationCoordinator]. This class owns the
 * common preparation and materialisation pipeline used by both isolated and coalesced execution,
 * so hook-selected instructions and source metadata are never recomputed during fallback.
 */
internal class NadelNewBatchHydrator(
    private val engine: NextgenEngine,
) {
    data class Invocation(
        val id: Int,
        val state: State,
        val executionBlueprint: NadelOverallExecutionBlueprint,
        /**
         * Stable source nodes together with their concrete client-response locations.
         */
        val sourceOccurrences: List<NadelResultOccurrence>,
    )

    /**
     * Holds hydration data about a given source object.
     */
    internal data class SourceObjectMetadata(
        val sourceOccurrence: NadelResultOccurrence,
        val sourceInputs: List<SourceInput>?,
    ) {
        val sourceObject: JsonNode
            get() = sourceOccurrence.node
    }

    internal sealed class SourceInput {
        abstract val sourceInputNode: JsonNode

        data class NotQueryable(
            override val sourceInputNode: JsonNode,
        ) : SourceInput()

        data class Queryable(
            override val sourceInputNode: JsonNode,
            val instruction: NadelBatchHydrationFieldInstruction,
            val indexKey: NadelBatchHydrationIndexKey,
        ) : SourceInput()
    }

    /**
     * The complete, reusable preparation result for one hydration invocation.
     *
     * [sourceInputsByInstruction] is consumed directly by isolated execution, while
     * [sourceObjectsMetadata] and [sourceInputs] are consumed by shared planning and
     * materialisation.
     */
    internal data class PreparedBatchHydration(
        val invocation: Invocation,
        val context: NadelBatchHydratorContext,
        val sourceObjectsMetadata: List<SourceObjectMetadata>,
        val sourceInputs: List<SourceInput>,
        val sourceInputsByInstruction: Map<NadelBatchHydrationFieldInstruction, List<SourceInput>>,
    )

    /**
     * Executes the scalar result-transform path through the same preparation pipeline.
     */
    suspend fun hydrate(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        sourceObjects: List<JsonNode>,
    ): List<NadelResultInstruction> {
        return hydrate(
            prepare(
                Invocation(
                    id = 0,
                    state = state,
                    executionBlueprint = executionBlueprint,
                    sourceOccurrences = sourceObjects.map { sourceObject ->
                        NadelResultOccurrence(
                            node = sourceObject,
                        )
                    },
                ),
            ),
        )
    }

    /**
     * Resolves all source metadata and hook-selected instructions once, before the invocation is
     * assigned to either isolated or coalesced execution.
     */
    internal fun prepare(
        invocation: Invocation,
    ): PreparedBatchHydration {
        val state = invocation.state
        val context = NadelBatchHydratorContext(
            instructionsByObjectTypeNames = state.instructionsByObjectTypeNames,
            executionContext = state.executionContext,
            sourceField = state.virtualField,
            sourceFieldService = state.virtualFieldService,
            aliasHelper = state.aliasHelper,
            executionBlueprint = invocation.executionBlueprint,
        )

        return with(context) {
            val sourceObjectsMetadata = getSourceObjectsMetadata(
                sourceOccurrences = invocation.sourceOccurrences,
            )
            val sourceInputs = sourceObjectsMetadata.flatMap { metadata ->
                metadata.sourceInputs.orEmpty()
            }

            PreparedBatchHydration(
                invocation = invocation,
                context = context,
                sourceObjectsMetadata = sourceObjectsMetadata,
                sourceInputs = sourceInputs,
                sourceInputsByInstruction = groupSourceInputsByInstruction(sourceObjectsMetadata),
            )
        }
    }

    /**
     * Executes a prepared invocation using the ordinary isolated hydration behavior.
     */
    internal suspend fun hydrate(
        hydration: PreparedBatchHydration,
    ): List<NadelResultInstruction> {
        return with(hydration.context) {
            if (isDeferred()) {
                deferHydrations(
                    sourceInputsByInstruction = hydration.sourceInputsByInstruction,
                    sourceObjectsMetadata = hydration.sourceObjectsMetadata,
                )
                emptyList()
            } else {
                val resultsByInstruction = executeHydrations(
                    hydration.sourceInputsByInstruction,
                )
                val indexedResultsByInstruction = getIndexedResultsByInstruction(
                    resultsByInstruction,
                )
                val setData = getSetDataInstructions(
                    sourceObjectsMetadata = hydration.sourceObjectsMetadata,
                    indexedResultsByInstruction = indexedResultsByInstruction,
                )
                val addErrors = resultsByInstruction
                    .flatMap { (_, results) ->
                        getInstructionsToAddErrors(results)
                    }

                setData + addErrors
            }
        }
    }

    internal fun indexSharedResults(
        instruction: NadelBatchHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        objectIdentifiers: List<NadelBatchHydrationMatchStrategy.MatchObjectIdentifier>,
        batches: List<NadelResolvedObjectBatch>,
    ): Map<NadelBatchHydrationIndexKey, JsonNode> {
        return NadelBatchHydrationObjectIdentifiedIndexer(
            instruction = instruction,
            aliasHelper = aliasHelper,
            strategy = NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(objectIdentifiers),
        ).getIndex(batches)
    }

    internal fun materializeSharedResults(
        hydration: PreparedBatchHydration,
        instruction: NadelBatchHydrationFieldInstruction,
        indexedResults: Map<NadelBatchHydrationIndexKey, JsonNode>,
    ): List<NadelResultInstruction> {
        return with(hydration.context) {
            getSetDataInstructions(
                sourceObjectsMetadata = hydration.sourceObjectsMetadata,
                indexedResultsByInstruction = mapOf(
                    instruction to indexedResults,
                ),
                copyIndexedResults = true,
            )
        }
    }

    context(NadelBatchHydratorContext)
    private fun deferHydrations(
        sourceInputsByInstruction: Map<NadelBatchHydrationFieldInstruction, List<SourceInput>>,
        sourceObjectsMetadata: List<SourceObjectMetadata>,
    ) {
        executionContext.incrementalResultSupport.defer {
            val resultsByInstruction = executeHydrations(sourceInputsByInstruction)
            val indexedResultsByInstruction = getIndexedResultsByInstruction(resultsByInstruction)

            val incremental = sourceObjectsMetadata
                .mapNotNull { metadata ->
                    makeDeferPayload(
                        metadata.sourceObject,
                        indexedResultsByInstruction,
                        metadata.sourceInputs,
                    )
                }

            DelayedIncrementalPartialResultImpl.Builder()
                .incrementalItems(incremental)
                .build()
        }
    }

    context(NadelBatchHydratorContext)
    private suspend fun makeDeferPayload(
        sourceObject: JsonNode,
        indexedResultsByInstruction: Map<NadelBatchHydrationFieldInstruction, Map<NadelBatchHydrationIndexKey, JsonNode>>,
        sourceInputsPairedWithInstruction: List<SourceInput>?,
    ): DeferPayload? {
        val sourceObjectResultPath = executionContext.resultTracker.getResultPath(
            sourceField.queryPath.dropLast(n = 1),
            sourceObject,
        )

        // TODO: Extract to Utils somewhere
        // This isn't really right… but we start with this
        val label = sourceField.deferredExecutions.firstNotNullOfOrNull { it.label }

        return if (sourceObjectResultPath == null) {
            null
        } else {
            DeferPayload.Builder()
                .path(sourceObjectResultPath.toRawPath())
                .label(label)
                .data(
                    mapOf(
                        sourceField.resultKey to getHydrationValueForSourceObject(
                            indexedResultsByInstruction,
                            sourceInputsPairedWithInstruction,
                        ).value,
                    ),
                )
                .build()
        }
    }

    context(NadelBatchHydratorContext)
    private fun isDeferred(): Boolean {
        return executionContext.hints.deferSupport()
            && executionContext.hydrationDetails == null // No nested hydrations
            && sourceField.deferredExecutions.isNotEmpty()
    }

    context(NadelBatchHydratorContext)
    private fun getSetDataInstructions(
        sourceObjectsMetadata: List<SourceObjectMetadata>,
        indexedResultsByInstruction: Map<NadelBatchHydrationFieldInstruction, Map<NadelBatchHydrationIndexKey, JsonNode>>,
        copyIndexedResults: Boolean = false,
    ): List<NadelResultInstruction> {
        return sourceObjectsMetadata
            .map { metadata ->
                NadelResultInstruction.Set(
                    subject = metadata.sourceObject,
                    field = sourceField,
                    newValue = getHydrationValueForSourceObject(
                        indexedResultsByInstruction,
                        metadata.sourceInputs,
                        copyIndexedResults,
                    ),
                )
            }
    }

    context(NadelBatchHydratorContext)
    private fun getHydrationValueForSourceObject(
        indexedResultsByInstruction: Map<NadelBatchHydrationFieldInstruction, Map<NadelBatchHydrationIndexKey, JsonNode>>,
        sourceInputsPairedWithInstruction: List<SourceInput>?,
        copyIndexedResults: Boolean = false,
    ): JsonNode {
        fun extractNode(sourceInput: SourceInput): JsonNode {
            return when (sourceInput) {
                is SourceInput.NotQueryable -> JsonNode.Null
                is SourceInput.Queryable -> {
                    val indexedNode =
                        indexedResultsByInstruction[sourceInput.instruction]!![sourceInput.indexKey]
                            ?: return JsonNode.Null
                    if (copyIndexedResults) {
                        JsonNode(deepCopyJsonValue(indexedNode.value))
                    } else {
                        indexedNode
                    }
                }
            }
        }

        return if (isIndexHydration) {
            if (sourceInputsPairedWithInstruction == null) {
                JsonNode.Null
            } else if (isSourceFieldListOutput) {
                if (isSourceInputFieldListOutput) {
                    JsonNode(
                        sourceInputsPairedWithInstruction
                            .map { sourceInput ->
                                extractNode(sourceInput).value
                            },
                    )
                } else {
                    val sourceInput = sourceInputsPairedWithInstruction.single()
                    extractNode(sourceInput)
                }
            } else {
                if (sourceInputsPairedWithInstruction.isEmpty()) {
                    JsonNode.Null
                } else {
                    val sourceInput = sourceInputsPairedWithInstruction.single()
                    extractNode(sourceInput)
                }
            }
        } else {
            if (sourceInputsPairedWithInstruction == null) {
                JsonNode.Null
            } else if (isSourceFieldListOutput) {
                JsonNode(
                    sourceInputsPairedWithInstruction
                        .map { sourceInput ->
                            extractNode(sourceInput).value
                        },
                )
            } else {
                if (sourceInputsPairedWithInstruction.isEmpty()) {
                    JsonNode.Null
                } else {
                    val sourceInput = sourceInputsPairedWithInstruction.single()
                    extractNode(sourceInput)
                }
            }
        }
    }

    context(NadelBatchHydratorContext)
    private fun getIndexer(
        instruction: NadelBatchHydrationFieldInstruction,
    ): NadelBatchHydrationIndexer {
        return when (val matchStrategy = instruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> NadelBatchHydrationIndexBasedIndexer(
                instruction = instruction,
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> NadelBatchHydrationObjectIdentifiedIndexer(
                aliasHelper = aliasHelper,
                instruction = instruction,
                strategy = matchStrategy,
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> NadelBatchHydrationObjectIdentifiedIndexer(
                aliasHelper = aliasHelper,
                instruction = instruction,
                strategy = matchStrategy,
            )
        }
    }

    context(NadelBatchHydratorContext)
    private suspend fun executeHydrations(
        sourceInputsByInstruction: Map<NadelBatchHydrationFieldInstruction, List<SourceInput>>,
    ): Map<NadelBatchHydrationFieldInstruction, List<NadelResolvedObjectBatch>> {
        // It's important to ensure deferredResults is a List not a Sequence. We want to kick off
        // all hydrations at the same time, then wait for them.
        val deferredResults = coroutineScope {
            sourceInputsByInstruction
                .entries
                .map { (instruction, sourceInputs) ->
                    instruction to async {
                        executeQueries(
                            executionBlueprint = executionBlueprint,
                            instruction = instruction,
                            sourceInputs = sourceInputs,
                        )
                    }
                }
        }

        return deferredResults
            .associate { (instruction, deferred) ->
                instruction to deferred.await()
            }
    }

    context(NadelBatchHydratorContext)
    private suspend fun executeQueries(
        executionBlueprint: NadelOverallExecutionBlueprint,
        instruction: NadelBatchHydrationFieldInstruction,
        sourceInputs: List<SourceInput>,
    ): List<NadelResolvedObjectBatch> {
        val uniqueSourceInputs = sourceInputs
            .asSequence()
            // We don't want to query for null values, we always map those to null
            .filter {
                it.sourceInputNode.value != null
            }
            .map {
                it.sourceInputNode
            }
            .toCollection(LinkedHashSet())
            .toList()

        val argBatches = NadelNewBatchHydrationInputBuilder.getInputValueBatches(
            hooks = executionContext.hooks,
            userContext = executionContext.userContext,
            instruction = instruction,
            hydrationField = sourceField,
            sourceInputs = uniqueSourceInputs,
        )

        val queries = NadelHydrationFieldsBuilder
            .makeBatchBackingQueries(
                executionHints = executionContext.hints,
                executionBlueprint = executionBlueprint,
                instruction = instruction,
                aliasHelper = aliasHelper,
                virtualField = sourceField,
                argBatches = argBatches.map { it.arguments },
            )

        return coroutineScope {
            queries
                .map { query ->
                    async {
                        val hydrationSourceService = executionBlueprint.getServiceOwning(instruction.location)!!
                        val hydrationBackingField =
                            FieldCoordinates.coordinates(instruction.backingFieldContainer, instruction.backingFieldDef)

                        val serviceHydrationDetails = ServiceExecutionHydrationDetails(
                            instruction = instruction,
                            timeout = instruction.timeout,
                            batchSize = instruction.batchSize,
                            hydrationSourceService = hydrationSourceService,
                            hydrationVirtualField = instruction.location,
                            hydrationBackingField = hydrationBackingField,
                            fieldPath = sourceField.listOfResultKeys,
                        )
                        engine.executeHydration(
                            service = instruction.backingService,
                            topLevelField = query,
                            executionContext = executionContext,
                            hydrationDetails = serviceHydrationDetails,
                        )
                    }
                }
                .awaitAll()
                // TODO: Output pairs of argument batches and queries from the fields builder.
                .asSequence()
                .zipOrThrow(argBatches) {
                    error("Each argument batch must correspond to one query")
                }
                .map { (result, argBatch) ->
                    NadelResolvedObjectBatch(argBatch.sourceInputs, result)
                }
                .toList()
        }
    }

    context(NadelBatchHydratorContext)
    private fun getSourceObjectsMetadata(
        sourceOccurrences: List<NadelResultOccurrence>,
    ): List<SourceObjectMetadata> {
        return sourceOccurrences
            .mapNotNull { sourceOccurrence ->
                val sourceObject = sourceOccurrence.node
                val instructions = instructionsByObjectTypeNames.getInstructionsForNode(
                    executionBlueprint = executionBlueprint,
                    service = sourceFieldService,
                    aliasHelper = aliasHelper,
                    parentNode = sourceObject,
                )

                if (instructions.isEmpty()) {
                    null
                } else {
                    val sourceInputs = getSourceInputs(
                        sourceObject = sourceObject,
                        instructions = instructions,
                    )

                    SourceObjectMetadata(
                        sourceOccurrence = sourceOccurrence,
                        sourceInputs = sourceInputs,
                    )
                }
            }
    }

    context(NadelBatchHydratorContext)
    private fun getSourceInputs(
        sourceObject: JsonNode,
        instructions: List<NadelBatchHydrationFieldInstruction>,
    ): List<SourceInput>? {
        val coords = instructions.first().location

        return if (executionBlueprint.engineSchema.getField(coords)!!.type.unwrapNonNull().isList) {
            val fieldSource = instructions
                .first()
                .backingFieldArguments
                .asSequence()
                .map {
                    it.valueSource
                }
                .singleOfType<ValueSource.FieldResultValue>()

            getSourceInputNodes(sourceObject, fieldSource, aliasHelper, includeNulls = isIndexHydration)
                ?.map { sourceInput ->
                    val instruction =
                        getHydrationInstructionForSourceInput(instructions, sourceObject, sourceInput, fieldSource)
                    if (instruction == null) {
                        SourceInput.NotQueryable(sourceInput)
                    } else {
                        SourceInput.Queryable(
                            sourceInputNode = sourceInput,
                            instruction = instruction,
                            indexKey = getIndexer(instruction).getIndexKey(sourceInput),
                        )
                    }
                }
        } else {
            // TODO: Determine what to do here in the longer term. This hook should probably be replaced.
            val instruction = getHydrationInstructionForSourceObject(instructions, sourceObject)

            if (instruction == null) {
                null
            } else {
                val fieldSource = instruction
                    .backingFieldArguments
                    .asSequence()
                    .map {
                        it.valueSource
                    }
                    .singleOfType<ValueSource.FieldResultValue>()

                getSourceInputNodes(sourceObject, fieldSource, aliasHelper, includeNulls = isIndexHydration)
                    ?.map { sourceInput ->
                        SourceInput.Queryable(
                            sourceInputNode = sourceInput,
                            instruction = instruction,
                            indexKey = getIndexer(instruction).getIndexKey(sourceInput),
                        )
                    }
            }
        }
    }

    context(NadelBatchHydratorContext)
    private fun getHydrationInstructionForSourceInput(
        instructions: List<NadelBatchHydrationFieldInstruction>,
        sourceObject: JsonNode,
        sourceInput: JsonNode,
        fieldSource: ValueSource.FieldResultValue,
    ): NadelBatchHydrationFieldInstruction? {
        if (instructions.any { it.condition == null }) {
            return executionContext.hooks.getHydrationInstruction(
                virtualField = sourceField,
                instructions = instructions,
                sourceInput = sourceInput,
                userContext = executionContext.userContext,
            )
        }

        return instructions.firstOrNull {
            // Validation guarantees that all instructions here have a condition.
            val condition = it.condition!!
            if (condition.fieldPath == fieldSource.queryPathToField) {
                it.condition.evaluate(sourceInput.value)
            } else {
                val resultQueryPath = aliasHelper.getQueryPath(condition.fieldPath)
                val node = JsonNodeExtractor.getNodesAt(sourceObject, resultQueryPath)
                    .emptyOrSingle()
                it.condition.evaluate(node?.value)
            }
        }
    }

    context(NadelBatchHydratorContext)
    private fun getHydrationInstructionForSourceObject(
        instructions: List<NadelBatchHydrationFieldInstruction>,
        sourceObject: JsonNode,
    ): NadelBatchHydrationFieldInstruction? {
        if (instructions.any { it.condition == null }) {
            return executionContext.hooks.getHydrationInstruction(
                virtualField = sourceField,
                instructions = instructions,
                parentNode = sourceObject,
                aliasHelper = aliasHelper,
                userContext = executionContext.userContext,
            )
        }

        return instructions.firstOrNull {
            // Validation guarantees that all instructions here have a condition.
            val resultQueryPath = aliasHelper.getQueryPath(it.condition!!.fieldPath)
            val node = JsonNodeExtractor.getNodesAt(sourceObject, resultQueryPath)
                .emptyOrSingle()
            it.condition.evaluate(node?.value)
        }
    }

    /**
     * Groups the [SourceInput] by instruction so that all source IDs for one backing query can be
     * gathered together.
     */
    private fun groupSourceInputsByInstruction(
        sourceObjects: List<SourceObjectMetadata>,
    ): Map<NadelBatchHydrationFieldInstruction, List<SourceInput>> {
        return sourceObjects
            .asSequence()
            .flatMap {
                it.sourceInputs ?: emptyList()
            }
            .filterIsInstance<SourceInput.Queryable>()
            .groupBy { sourceInput ->
                sourceInput.instruction
            }
    }

    /**
     * Gets the [JsonNode] source inputs for [sourceObject].
     */
    private fun getSourceInputNodes(
        sourceObject: JsonNode,
        valueSource: ValueSource.FieldResultValue,
        aliasHelper: NadelAliasHelper,
        includeNulls: Boolean,
    ): List<JsonNode>? {
        val resultPath = aliasHelper.getQueryPath(valueSource.queryPathToField)
        @Suppress("DEPRECATION") // TODO: Move this to the new JsonNodes API.
        return JsonNodeExtractor.getNodesAt(sourceObject, resultPath, flatten = true)
            .also {
                if (it.isNotEmpty() && it.all { node -> node.value == null }) {
                    return null
                }
            }
            .asSequence()
            .map { it.value }
            .flatten(recursively = true)
            .let {
                if (includeNulls) {
                    it
                } else {
                    it.filterNotNull()
                }
            }
            .map(::JsonNode)
            .toList()
    }

    context(NadelBatchHydratorContext)
    private fun getIndexedResultsByInstruction(
        resultsByInstruction: Map<NadelBatchHydrationFieldInstruction, List<NadelResolvedObjectBatch>>,
    ): Map<NadelBatchHydrationFieldInstruction, Map<NadelBatchHydrationIndexKey, JsonNode>> {
        return resultsByInstruction
            .mapValues { (instruction, results) ->
                getIndexer(instruction).getIndex(results)
            }
    }
}

/**
 * Stores common information used while preparing, executing and materialising one hydration.
 */
internal class NadelBatchHydratorContext(
    val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelBatchHydrationFieldInstruction>>,
    val executionContext: NadelExecutionContext,
    val sourceField: ExecutableNormalizedField,
    val sourceFieldService: Service,
    val aliasHelper: NadelAliasHelper,
    val executionBlueprint: NadelOverallExecutionBlueprint,
) {
    val isSourceFieldListOutput: Boolean by lazy {
        // Abstract output types must all define the same list wrapping.
        val instruction = instructionsByObjectTypeNames.values.first().first()
        executionBlueprint.engineSchema.getField(instruction.location)!!.type.unwrapNonNull().isList
    }

    val isSourceInputFieldListOutput: Boolean by lazy {
        // TODO: This assumption feels wrong and instructions are not necessarily identical.
        instructionsByObjectTypeNames.values.first()
            .any { instruction ->
                instruction.backingFieldArguments
                    .asSequence()
                    .map {
                        it.valueSource
                    }
                    .filterIsInstance<ValueSource.FieldResultValue>()
                    .any { fromSourceInputField ->
                        fromSourceInputField.fieldDefinition.type.unwrapNonNull().isList
                    }
            }
    }

    val isIndexHydration: Boolean by lazy {
        // If one instruction is index based then validation requires all of them to be.
        instructionsByObjectTypeNames.values.first()
            .any {
                it.batchHydrationMatchStrategy is NadelBatchHydrationMatchStrategy.MatchIndex
            }
    }
}
