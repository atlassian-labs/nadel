package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef.ValueSource
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.getField
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.engine.util.unwrapNonNull
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class NadelNewBatchHydrator(
    private val engine: NextgenEngine,
) {
    private data class ObjectIdentifier(
        val data: Any?,
    )

    /**
     * todo: add validation that repeated directives must use the same $source object unless there is only one input
     */
    suspend fun hydrate(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val parentNodeHydrationSetups = getParentNodeHydrationSetup(
            state,
            executionBlueprint,
            parentNodes,
        )

        val sourceIdsByInstruction = parentNodeHydrationSetups
            .flatMap {
                it.sourceIds
            }
            .groupBy(
                keySelector = { (_, instruction) ->
                    instruction
                },
                valueTransform = { (sourceId, _) ->
                    sourceId
                },
            )

        val indexedResults = sourceIdsByInstruction
            .mapValues { (instruction, sourceIds) ->
                val results = executeQueries(
                    state = state,
                    executionBlueprint = executionBlueprint,
                    instruction = instruction,
                    sourceIds = sourceIds,
                )

                indexResults(state.aliasHelper, instruction, results)
            }

        val isHydratedFieldListOutput = executionBlueprint.engineSchema
            .getField(
                makeFieldCoordinates(
                    typeName = state.hydratedField.objectTypeNames.first(),
                    fieldName = state.hydratedField.name,
                )
            )!!.type.unwrapNonNull().isList

        return parentNodeHydrationSetups
            .map { (parentNode, sourceIdsPairedWithInstruction) ->
                fun extractNode(sourceId: JsonNode, instruction: NadelBatchHydrationFieldInstruction): JsonNode? {
                    return indexedResults[instruction]!![ObjectIdentifier(sourceId.value)]
                }

                val value: Any? = if (isHydratedFieldListOutput) {
                    sourceIdsPairedWithInstruction
                        .map { (sourceId, instruction) ->
                            extractNode(sourceId, instruction)?.value
                        }
                } else {
                    val (sourceId, instruction) = sourceIdsPairedWithInstruction.single()
                    extractNode(sourceId, instruction)?.value
                }

                NadelResultInstruction.Set(
                    subject = parentNode,
                    key = NadelResultKey(state.hydratedField.resultKey),
                    newValue = JsonNode(value),
                )
            }
    }

    private fun indexResults(
        aliasHelper: NadelAliasHelper,
        instruction: NadelBatchHydrationFieldInstruction,
        results: List<ServiceExecutionResult>,
    ): Map<ObjectIdentifier, JsonNode> {
        return when (val strategy = instruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> {
                throw UnsupportedOperationException("todo")
            }
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> {
                results
                    .flatMap { result ->
                        JsonNodeExtractor.getNodesAt(result.data, instruction.queryPathToActorField, flatten = true)
                    }
                    .groupBy { node ->
                        @Suppress("UNCHECKED_CAST")
                        ObjectIdentifier(
                            // Remove result ID after using it to create this index to stop it showing up in end result
                            (node.value as MutableJsonMap).remove(aliasHelper.getResultKey(strategy.resultId)),
                        )
                    }
                    .mapValues { (_, values) ->
                        // todo: stop doing stupid here
                        values.single()
                    }
            }
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> {
                throw UnsupportedOperationException("todo")
            }
        }
    }

    private suspend fun executeQueries(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        instruction: NadelBatchHydrationFieldInstruction,
        sourceIds: List<JsonNode>,
    ): List<ServiceExecutionResult> {
        val argBatches = NadelNewBatchHydrationInputBuilder.getInputValueBatches(
            hooks = state.executionContext.hooks,
            userContext = state.executionContext.userContext,
            instruction = instruction,
            hydrationField = state.hydratedField,
            sourceIds = sourceIds,
        )

        val queries = NadelHydrationFieldsBuilder
            .makeBatchActorQueries(
                executionBlueprint = executionBlueprint,
                instruction = instruction,
                aliasHelper = state.aliasHelper,
                hydratedField = state.hydratedField,
                argBatches = argBatches,
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
                            fieldPath = state.hydratedField.listOfResultKeys,
                        )
                        engine.executeTopLevelField(
                            service = instruction.actorService,
                            topLevelField = query,
                            executionContext = state.executionContext,
                            serviceHydrationDetails = serviceHydrationDetails,
                        )
                    }
                }
        }.awaitAll()
    }

    private data class ParentNodeHydrationSetup(
        val parentNode: JsonNode,
        val sourceIds: List<Pair<JsonNode, NadelBatchHydrationFieldInstruction>>,
    )

    private fun getParentNodeHydrationSetup(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        parentNodes: List<JsonNode>,
    ): List<ParentNodeHydrationSetup> {
        return parentNodes
            .map { parentNode ->
                val instructions = state.instructionsByObjectTypeNames.getInstructionsForNode(
                    executionBlueprint = executionBlueprint,
                    service = state.hydratedFieldService,
                    aliasHelper = state.aliasHelper,
                    parentNode = parentNode,
                )

                val sourceIdsPairedWithInstructions = getInstructionParingForSourceIds(
                    state = state,
                    executionBlueprint = executionBlueprint,
                    parentNode = parentNode,
                    instructions = instructions,
                )

                ParentNodeHydrationSetup(
                    parentNode,
                    sourceIdsPairedWithInstructions,
                )
            }
    }

    private fun getInstructionParingForSourceIds(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        parentNode: JsonNode,
        instructions: List<NadelBatchHydrationFieldInstruction>,
    ): List<Pair<JsonNode, NadelBatchHydrationFieldInstruction>> {
        val coords = makeFieldCoordinates(
            typeName = state.hydratedField.objectTypeNames.first(),
            fieldName = state.hydratedField.name,
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

            // todo: move this to validation
            instructions
                .forEach { instruction ->
                    instruction.actorInputValueDefs.single { arg ->
                        arg.valueSource == fieldSource
                    }
                }

            extractValues(parentNode, fieldSource, state.aliasHelper)
                .map { sourceId ->
                    // todo: handle null here
                    val instruction = state.executionContext.hooks.getHydrationInstruction(
                        instructions = instructions,
                        sourceId = sourceId,
                        userContext = state.executionContext.userContext,
                    )!!

                    sourceId to instruction
                }
        } else {
            // todo: determine what to do here in the longer term, this hook should probably be replaced
            val instruction = state.executionContext.hooks.getHydrationInstruction(
                instructions = instructions,
                parentNode = parentNode,
                aliasHelper = state.aliasHelper,
                userContext = state.executionContext.userContext,
            )!!

            extractValues(parentNode, fieldSource, state.aliasHelper)
                .map { sourceId ->
                    sourceId to instruction
                }
        }
    }

    private fun extractValues(
        parentNode: JsonNode,
        valueSource: ValueSource.FieldResultValue,
        aliasHelper: NadelAliasHelper,
    ): List<JsonNode> {
        val resultPath = aliasHelper.getQueryPath(valueSource.queryPathToField)
        @Suppress("DEPRECATION") // todo: maybe un-deprecate this or move to new JsonNodes
        return JsonNodeExtractor.getNodesAt(parentNode, resultPath, flatten = true)
            .asSequence()
            .map { it.value }
            .flatten(recursively = true)
            .map {
                JsonNode(it)
            }
            .toList()
    }
}
