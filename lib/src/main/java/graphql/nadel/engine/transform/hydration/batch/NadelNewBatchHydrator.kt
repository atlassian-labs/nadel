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
import graphql.nadel.engine.util.emptyOrSingle
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
 * 2. There can be multiple source inputs to hydrate per result object e.g. one issue may have multiple contributors
 * 3. Each source input can resolve to a different @hydrated instruction
 *
 * So what do we need
 *
 * 1. Source object -> Source inputs
 * 2. Source input -> @hydrated instruction
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
 * 1. Source object -> Source inputs [getSourceObjectsMetadata]
 * 2. Source input -> instruction [getSourceInputs]
 * 3. Resolved objects need to be indexed [getIndexedResultsByInstruction]
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
 *      "sourceInputs": [
 *          {
 *              sourceInputNode: "user/2"
 *              instruction: "@hydrated(field: userById)"
 *              indexKey: "user/2"
 *          }
 *          {
 *              sourceInputNode: "comment/4"
 *              instruction: "@hydrated(field: commentById)"
 *              indexKey: "comment/4"
 *          }
 *      ]
 *   }
 *   {
 *      "sourceObject": {
 *         "key": "ZELDA-12"
 *         "linkIds": ["user/128"]
 *      }
 *      "sourceInputs": [
 *          {
 *              sourceInputNode: "user/122"
 *              instruction: "@hydrated(field: userById)"
 *              indexKey: "comment/4"
 *          }
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
 *     .sourceInputs
 *     .map { sourceInput ->
 *       index[sourceInput.instruction][sourceInput.indexKey]
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
        val sourceInputs: List<SourceInput>?,
    )

    private sealed class SourceInput {
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
        // Gets source inputs, instructions info etc.
        val sourceObjectsMetadata = getSourceObjectsMetadata(sourceObjects)
        val sourceInputsByInstruction = groupSourceInputsByInstruction(sourceObjectsMetadata)

        val resultsByInstruction = sourceInputsByInstruction
            .mapValues { (instruction, sourceInputs) ->
                executeQueries(
                    executionBlueprint = executionBlueprint,
                    instruction = instruction,
                    sourceInputs = sourceInputs,
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
            .map { (sourceObject, sourceInputsPairedWithInstruction) ->
                NadelResultInstruction.Set(
                    subject = sourceObject,
                    field = sourceField,
                    newValue = getHydrationValueForSourceObject(
                        indexedResultsByInstruction,
                        sourceInputsPairedWithInstruction,
                    ),
                )
            }
    }

    context(NadelBatchHydratorContext)
    private fun getHydrationValueForSourceObject(
        indexedResultsByInstruction: Map<NadelBatchHydrationFieldInstruction, Map<NadelBatchHydrationIndexKey, JsonNode>>,
        sourceInputsPairedWithInstruction: List<SourceInput>?,
    ): JsonNode {
        fun extractNode(sourceInput: SourceInput): JsonNode {
            return when (sourceInput) {
                is SourceInput.NotQueryable -> JsonNode.Null
                is SourceInput.Queryable -> indexedResultsByInstruction[sourceInput.instruction]!![sourceInput.indexKey]
                    ?: JsonNode.Null
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
                    NadelResolvedObjectBatch(argBatch.sourceInputs, result)
                }
                .toList()
        }
    }

    context(NadelBatchHydratorContext)
    private fun getSourceObjectsMetadata(
        sourceObjects: List<JsonNode>
    ): List<SourceObjectMetadata> {
        return sourceObjects
            .mapNotNull { sourceObject ->
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
                        instructions = instructions
                    )

                    SourceObjectMetadata(
                        sourceObject,
                        sourceInputs,
                    )
                }
            }
    }

    context(NadelBatchHydratorContext)
    private fun getSourceInputs(
        sourceObject: JsonNode,
        instructions: List<NadelBatchHydrationFieldInstruction>
    ): List<SourceInput>? {
        val coords = makeFieldCoordinates(
            typeName = sourceField.objectTypeNames.first(),
            fieldName = sourceField.name,
        )

        return if (executionBlueprint.engineSchema.getField(coords)!!.type.unwrapNonNull().isList) {
            val fieldSource = instructions
                .first()
                .actorInputValueDefs
                .asSequence()
                .map {
                    it.valueSource
                }
                .singleOfType<ValueSource.FieldResultValue>()

            getSourceInputNodes(sourceObject, fieldSource, aliasHelper, includeNulls = isIndexHydration)
                ?.map { sourceInput ->
                    val instruction = getFilteredInstructionFromList(instructions, sourceObject, sourceInput, fieldSource)
                    if (instruction == null) {
                        SourceInput.NotQueryable(sourceInput)
                    } else {
                        SourceInput.Queryable(
                            sourceInputNode = sourceInput,
                            instruction = instruction,
                            indexKey = getIndexer(instruction).getSourceKey(sourceInput),
                        )
                    }
                }
        } else {
            // todo: determine what to do here in the longer term, this hook should probably be replaced
            val instruction = getFilteredInstruction(instructions, sourceObject)

            if (instruction == null) {
                null
            } else {
                val fieldSource = instruction
                    .actorInputValueDefs
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
                            indexKey = getIndexer(instruction).getSourceKey(sourceInput),
                        )
                    }
            }
        }
    }

    context(NadelBatchHydratorContext)
    private fun getFilteredInstructionFromList(
        instructions: List<NadelBatchHydrationFieldInstruction>,
        sourceObject: JsonNode,
        sourceInput: JsonNode,
        fieldSource: ValueSource.FieldResultValue
    ): NadelBatchHydrationFieldInstruction? {
        if (instructions.any { it.condition == null }) {
            return executionContext.hooks.getHydrationInstruction(
                instructions = instructions,
                sourceInput = sourceInput,
                userContext = executionContext.userContext,
            )
        }

        return instructions.firstOrNull {
            // Note: due to the validation, all instructions in here have a condition, so can call explicitly
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
    private fun getFilteredInstruction(
        instructions: List<NadelBatchHydrationFieldInstruction>,
        sourceObject: JsonNode
    ): NadelBatchHydrationFieldInstruction? {
        if (instructions.any { it.condition == null }) {
            return executionContext.hooks.getHydrationInstruction(
                instructions = instructions,
                parentNode = sourceObject,
                aliasHelper = aliasHelper,
                userContext = executionContext.userContext,
            )
        }

        return instructions.firstOrNull {
            // Note: due to the validation, all instructions in here have a condition, so can call explicitly
            val resultQueryPath = aliasHelper.getQueryPath(it.condition!!.fieldPath)
            val node = JsonNodeExtractor.getNodesAt(sourceObject, resultQueryPath)
                .emptyOrSingle()
            it.condition.evaluate(node?.value)
        }
    }

    /**
     * Groups the [SourceInput] by instruction so that we can gather all the source
     * IDs together for a given query.
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
     * Gets the [JsonNode] source inputs for [sourceObject]
     */
    private fun getSourceInputNodes(
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
