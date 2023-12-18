package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef.ValueSource
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.engine.transform.hydration.batch.NadelNewBatchHydrator.SourceObjectMetadata
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexBasedIndexer
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexKey
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationIndexer
import graphql.nadel.engine.transform.hydration.batch.indexing.NadelBatchHydrationObjectIdentifiedIndexer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.PairList
import graphql.nadel.engine.util.filterPairSecondNotNull
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.getField
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.zipOrThrow
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * So this class performs batch hydration.
 *
 * Some things to consider
 *
 * 1. There can be repeated @hydrated instruction, so we need to choose one.
 * 2. There can be multiple source IDs to hydrate per result object e.g. one issue may have multiple contributors
 * 3. Each source ID can resolve to a different @hydrated instruction
 *
 * So what do we need
 *
 * 1. Source object -> Source IDs
 * 2. Source ID -> @hydrated instruction
 * 3. Resolved objects need to be indexed
 *
 * So let's consider a hydration
 *
 * ```graphql
 * union IssueLink = User | Comment
 * type Issue {
 *   key: String!
 *   linkIds: [IssueLink]
 *     @hydrated(
 *       field: "userById"
 *       arguments: [{name: "ids", value: "$source.linkIds"}]
 *     )
 *     @hydrated(
 *       field: "commentById"
 *       arguments: [{name: "ids", value: "$source.linkIds"}]
 *     )
 * }
 * ```
 *
 * Basically for given issues
 *
 * ```json
 * [
 *   {
 *      "key": "GQLGW-6"
 *      "linkIds": ["user/2", "comment/4"]
 *   },
 *   {
 *      "key": "ZELDA-12"
 *      "linkIds": ["user/128"]
 *   },
 * ]
 * ```
 *
 * We need to create
 *
 * 1. Source object -> Source IDs [getSourceObjectsMetadata]
 * 2. Source IDs to -> Instruction [getInstructionParingForSourceIds]
 * 3. Resolved objects need to be indexed [indexResults]
 *
 * e.g. of [SourceObjectMetadata]
 *
 * ```json
 * [
 *   {
 *      "sourceObject": {
 *         "key": "GQLGW-6"
 *         "linkIds": ["user/2", "comment/4"]
 *      }
 *      "sourceIds": [
 *          {"user/2": "@hydrated(field: userById)"}
 *          {"comment/4": "@hydrated(field: commentById)"}
 *      ]
 *   }
 *   {
 *      "sourceObject": {
 *         "key": "ZELDA-12"
 *         "linkIds": ["user/128"]
 *      }
 *      "sourceIds": [
 *          {"user/122": "@hydrated(field: userById)"}
 *      ]
 *   }
 * ]
 * ```
 *
 * e.g. of resolved index
 *
 * ```json
 * {
 *   "@hydrated(field: userById)": {
 *      "user/2": {"name": "Franklin"}
 *      "user/128": {"name": "Steven"}
 *   }
 *   "@hydrated(field: commentById)": {
 *     "comment/4": {"content": "Hello World"}
 *   }
 * }
 * ```
 *
 * Then we can loop through [SourceObjectMetadata] and look up the result index e.g.
 *
 * ```kotlin
 * for (sourceObjectMetadata in sourceObjectsMetadata) {
 *   val values = sourceObjectMetadata
 *     .sourceIdsPairedWithInstructions
 *     .map { (sourceId, instruction) ->
 *       index[instruction][sourceId]
 *     }
 * }
 * ```
 */
internal class NadelNewBatchHydrator(
    private val engine: NextgenEngine,
) {
    /**
     * Holds hydration data about a given source object.
     */
    private data class SourceObjectMetadata(
        val sourceObject: JsonNode,
        val sourceIdsPairedWithInstructions: PairList<JsonNode, NadelBatchHydrationFieldInstruction?>?,
    )

    /**
     * todo: add validation that repeated directives must use the same $source object unless there is only one input
     */
    suspend fun hydrate(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        sourceObjects: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val context = NadelBatchHydratorContext(
            instructionsByObjectTypeNames = state.instructionsByObjectTypeNames,
            executionContext = state.executionContext,
            sourceField = state.hydratedField,
            sourceFieldService = state.hydratedFieldService,
            aliasHelper = state.aliasHelper,
            executionBlueprint = executionBlueprint,
        )

        return with(context) {
            hydrate(sourceObjects)
        }
    }

    context(NadelBatchHydratorContext)
    suspend fun hydrate(sourceObjects: List<JsonNode>): List<NadelResultInstruction> {
        // Gets source IDs, instructions info etc.
        val sourceObjectsMetadata = getSourceObjectsMetadata(sourceObjects)
        val sourceIdsByInstruction = getSourceIdsByInstruction(sourceObjectsMetadata)

        val resultsByInstruction = sourceIdsByInstruction
            .mapValues { (instruction, sourceIds) ->
                executeQueries(
                    executionBlueprint = executionBlueprint,
                    instruction = instruction,
                    sourceIds = sourceIds,
                )
            }

        val indexedResultsByInstruction = getIndexedResultsByInstruction(resultsByInstruction)

        val setData = getSetDataInstructions(
            sourceObjectsMetadata = sourceObjectsMetadata,
            indexedResultsByInstruction = indexedResultsByInstruction,
        )

        val addErrors = resultsByInstruction
            .flatMap { (_, results) ->
                getInstructionsToAddErrors(results)
            }

        return setData + addErrors
    }

    context(NadelBatchHydratorContext)
    private fun getSetDataInstructions(
        sourceObjectsMetadata: List<SourceObjectMetadata>,
        indexedResultsByInstruction: Map<NadelBatchHydrationFieldInstruction, Map<NadelBatchHydrationIndexKey, JsonNode>>,
    ): List<NadelResultInstruction> {
        return sourceObjectsMetadata
            .map { (sourceObject, sourceIdsPairedWithInstruction) ->
                NadelResultInstruction.Set(
                    subject = sourceObject,
                    field = sourceField,
                    newValue = getHydrationValueForSourceObject(
                        indexedResultsByInstruction,
                        sourceIdsPairedWithInstruction,
                    ),
                )
            }
    }

    context(NadelBatchHydratorContext)
    private fun getHydrationValueForSourceObject(
        indexedResultsByInstruction: Map<NadelBatchHydrationFieldInstruction, Map<NadelBatchHydrationIndexKey, JsonNode>>,
        sourceIdsPairedWithInstruction: PairList<JsonNode, NadelBatchHydrationFieldInstruction?>?,
    ): JsonNode {
        fun extractNode(
            sourceId: JsonNode,
            instruction: NadelBatchHydrationFieldInstruction?,
        ): JsonNode {
            return if (instruction == null) {
                JsonNode.Null
            } else {
                val key = getIndexer(instruction).getSourceKey(sourceId)
                // todo: could this ever be null say if response failed?
                indexedResultsByInstruction[instruction]!![key] ?: JsonNode.Null
            }
        }

        return if (isIndexHydration) {
            if (sourceIdsPairedWithInstruction == null) {
                JsonNode.Null
            } else if (isSourceFieldListOutput) {
                if (isSourceInputFieldListOutput) {
                    JsonNode(
                        sourceIdsPairedWithInstruction
                            .map { (sourceId, instruction) ->
                                extractNode(sourceId, instruction).value
                            },
                    )
                } else {
                    val (sourceId, instruction) = sourceIdsPairedWithInstruction.single()
                    extractNode(sourceId, instruction)
                }
            } else {
                val (sourceId, instruction) = sourceIdsPairedWithInstruction.single()
                extractNode(sourceId, instruction)
            }
        } else {
            if (sourceIdsPairedWithInstruction == null) {
                JsonNode.Null
            } else if (isSourceFieldListOutput) {
                JsonNode(
                    sourceIdsPairedWithInstruction
                        .map { (sourceId, instruction) ->
                            extractNode(sourceId, instruction).value
                        },
                )
            } else {
                val (sourceId, instruction) = sourceIdsPairedWithInstruction.single()
                extractNode(sourceId, instruction)
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
    private suspend fun executeQueries(
        executionBlueprint: NadelOverallExecutionBlueprint,
        instruction: NadelBatchHydrationFieldInstruction,
        sourceIds: List<JsonNode>,
    ): List<NadelResolvedObjectBatch> {
        val uniqueSourceIds = sourceIds
            .asSequence()
            // We don't want to query for null values, we always map those to null
            .filter {
                it.value != null
            }
            .toSet()
            .toList()

        val argBatches = NadelNewBatchHydrationInputBuilder.getInputValueBatches(
            hooks = executionContext.hooks,
            userContext = executionContext.userContext,
            instruction = instruction,
            hydrationField = sourceField,
            sourceIds = uniqueSourceIds,
        )

        val queries = NadelHydrationFieldsBuilder
            .makeBatchActorQueries(
                executionBlueprint = executionBlueprint,
                instruction = instruction,
                aliasHelper = aliasHelper,
                hydratedField = sourceField,
                argBatches = argBatches.map { it.arguments },
            )

        return coroutineScope {
            queries
                .map { query ->
                    async { // This async executes the batches in parallel i.e. executes hydration as Deferred/Future
                        val hydrationSourceService = executionBlueprint.getServiceOwning(instruction.location)!!
                        val hydrationActorField =
                            FieldCoordinates.coordinates(instruction.actorFieldContainer, instruction.actorFieldDef)

                        val serviceHydrationDetails = ServiceExecutionHydrationDetails(
                            timeout = instruction.timeout,
                            batchSize = instruction.batchSize,
                            hydrationSourceService = hydrationSourceService,
                            hydrationSourceField = instruction.location,
                            hydrationActorField = hydrationActorField,
                            fieldPath = sourceField.listOfResultKeys,
                        )
                        engine.executeTopLevelField(
                            service = instruction.actorService,
                            topLevelField = query,
                            executionContext = executionContext,
                            serviceHydrationDetails = serviceHydrationDetails,
                        )
                    }
                }
                .awaitAll()
                // todo: in the future the output of NadelHydrationFieldsBuilder should be a pair of arg batch and query
                .asSequence()
                .zipOrThrow(argBatches) {
                    error("Each argument batch must correspond to one query")
                }
                .map { (result, argBatch) ->
                    NadelResolvedObjectBatch(argBatch.sourceIds, result)
                }
                .toList()
        }
    }

    context(NadelBatchHydratorContext)
    private fun getSourceObjectsMetadata(
        sourceObjects: List<JsonNode>,
    ): List<SourceObjectMetadata> {
        return sourceObjects
            .map { sourceObject ->
                val instructions = instructionsByObjectTypeNames.getInstructionsForNode(
                    executionBlueprint = executionBlueprint,
                    service = sourceFieldService,
                    aliasHelper = aliasHelper,
                    parentNode = sourceObject,
                )

                val sourceIdsPairedWithInstructions = getInstructionParingForSourceIds(
                    sourceObject = sourceObject,
                    instructions = instructions,
                )

                SourceObjectMetadata(
                    sourceObject,
                    sourceIdsPairedWithInstructions,
                )
            }
    }

    context(NadelBatchHydratorContext)
    private fun getInstructionParingForSourceIds(
        sourceObject: JsonNode,
        instructions: List<NadelBatchHydrationFieldInstruction>,
    ): PairList<JsonNode, NadelBatchHydrationFieldInstruction?>? {
        val coords = makeFieldCoordinates(
            typeName = sourceField.objectTypeNames.first(),
            fieldName = sourceField.name,
        )

        val fieldSource = instructions
            .first()
            .actorInputValueDefs
            .asSequence()
            .map {
                it.valueSource
            }
            .singleOfType<ValueSource.FieldResultValue>()

        return if (executionBlueprint.engineSchema.getField(coords)!!.type.unwrapNonNull().isList) {
            getSourceInputs(sourceObject, fieldSource, aliasHelper, includeNulls = isIndexHydration)
                ?.map { sourceId ->
                    val instruction = executionContext.hooks.getHydrationInstruction(
                        instructions = instructions,
                        sourceId = sourceId,
                        userContext = executionContext.userContext,
                    )

                    sourceId to instruction
                }
        } else {
            // todo: determine what to do here in the longer term, this hook should probably be replaced
            val instruction = executionContext.hooks.getHydrationInstruction(
                instructions = instructions,
                parentNode = sourceObject,
                aliasHelper = aliasHelper,
                userContext = executionContext.userContext,
            )

            if (instruction == null) {
                null
            } else {
                getSourceInputs(sourceObject, fieldSource, aliasHelper, includeNulls = isIndexHydration)
                    ?.map { sourceId ->
                        sourceId to instruction
                    }
            }
        }
    }

    /**
     * Creates a giant inverted Map of [SourceObjectMetadata.sourceIdsPairedWithInstructions]
     * where the instruction is the key and the value is the source ID.
     */
    private fun getSourceIdsByInstruction(
        sourceObjects: List<SourceObjectMetadata>,
    ): Map<NadelBatchHydrationFieldInstruction, List<JsonNode>> {
        return sourceObjects
            .asSequence()
            .flatMap {
                it.sourceIdsPairedWithInstructions ?: emptyList()
            }
            // Removes Pair values where instruction is null
            .filterPairSecondNotNull()
            .groupBy(
                // Pair<SourceId, Instruction>
                keySelector = { (_, instruction) ->
                    instruction
                },
                valueTransform = { (sourceId, _) ->
                    sourceId
                },
            )
    }

    /**
     * Gets the source inputs for [sourceObject]
     */
    private fun getSourceInputs(
        sourceObject: JsonNode,
        valueSource: ValueSource.FieldResultValue,
        aliasHelper: NadelAliasHelper,
        includeNulls: Boolean,
    ): List<JsonNode>? {
        val resultPath = aliasHelper.getQueryPath(valueSource.queryPathToField)
        @Suppress("DEPRECATION") // todo: maybe un-deprecate this or move to new JsonNodes
        return JsonNodeExtractor.getNodesAt(sourceObject, resultPath, flatten = true)
            .also {
                // Do nothing
                if (it.isNotEmpty() && it.all { it.value == null }) {
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
            .map {
                JsonNode(it)
            }
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
 * Stores common info to execute the hydration.
 *
 * Used as a context receiver to pass around common info.
 */
private class NadelBatchHydratorContext(
    val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelBatchHydrationFieldInstruction>>,
    val executionContext: NadelExecutionContext,
    val sourceField: ExecutableNormalizedField,
    val sourceFieldService: Service,
    val aliasHelper: NadelAliasHelper,
    val executionBlueprint: NadelOverallExecutionBlueprint,
) {
    val isSourceFieldListOutput: Boolean by lazy {
        executionBlueprint.engineSchema
            .getField(
                makeFieldCoordinates(
                    // In regard to the field output type, the abstract types must all define the same list wrapping
                    // So here, it does not matter which object type we inspect
                    typeName = sourceField.objectTypeNames.first(),
                    fieldName = sourceField.name,
                )
            )!!.type.unwrapNonNull().isList
    }

    val isSourceInputFieldListOutput: Boolean by lazy {
        // todo: this assumption feels wrong and instructions aren't likely to be the same
        instructionsByObjectTypeNames.values.first()
            .any { instruction ->
                instruction.actorInputValueDefs
                    .asSequence()
                    .map { it.valueSource }
                    .filterIsInstance<ValueSource.FieldResultValue>()
                    .any { fromSourceInputField ->
                        fromSourceInputField.fieldDefinition.type.unwrapNonNull().isList
                    }
            }
    }

    val isIndexHydration: Boolean by lazy {
        // We don't care which instruction it is, if one is index based hydration all of them must be
        instructionsByObjectTypeNames.values.first()
            .any {
                it.batchHydrationMatchStrategy is NadelBatchHydrationMatchStrategy.MatchIndex
            }
    }
}
