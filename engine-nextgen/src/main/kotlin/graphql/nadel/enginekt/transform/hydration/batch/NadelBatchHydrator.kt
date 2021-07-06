package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.transform.NadelTransformUtil
import graphql.nadel.enginekt.transform.getInstructionForNode
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.asMutable
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.asNullableJsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.subListOrNull
import graphql.nadel.enginekt.util.unwrapNonNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class NadelBatchHydrator(
    private val engine: NextgenEngine,
) {
    suspend fun hydrate(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val parentNodesByInstruction: Map<NadelBatchHydrationFieldInstruction, List<JsonNode>> = parentNodes
            .mapNotNull { parentNode ->
                val instruction = state.instructions.getInstructionForNode(
                    executionBlueprint = executionBlueprint,
                    service = state.hydratedFieldService,
                    aliasHelper = state.aliasHelper,
                    parentNode = parentNode,
                )
                when (instruction) {
                    null -> null
                    else -> parentNode to instruction // Becomes Pair<JsonNode, Instruction>
                }
            }
            // Becomes Map<Instruction, List<Pair<JsonNode, Instruction>>>
            .groupBy { pair ->
                pair.second
            }
            // Changes List<Pair<JsonNode, Instruction>> to List<JsonNode>
            .mapValues { pairs ->
                pairs.value.map { pair -> pair.first }
            }

        return parentNodesByInstruction.flatMap { (instruction, parentNodes) ->
            hydrate(executionBlueprint, state, instruction, parentNodes)
        }
    }

    private suspend fun hydrate(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val batches: List<ServiceExecutionResult> = executeBatches(
            state = state,
            instruction = instruction,
            parentNodes = parentNodes,
        )
        val resultNodes: List<JsonNode> = getHydrationActorResultNodes(instruction, batches)

        return when (val matchStrategy = instruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> getHydrateInstructionsMatchingIndex(
                state = state,
                instruction = instruction,
                parentNodes = parentNodes,
                resultNodes = resultNodes,
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> getHydrateInstructionsMatchingObjectId(
                executionBlueprint = executionBlueprint,
                state = state,
                instruction = instruction,
                parentNodes = parentNodes,
                resultNodes = resultNodes,
                matchStrategy = matchStrategy,
            )
        } + getInstructionsToAddErrors(batches)
    }

    private fun getHydrateInstructionsMatchingIndex(
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        resultNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val batchInputValueSource = NadelBatchHydrationInputBuilder.getBatchInputDef(instruction)?.second

        val resultValues = resultNodes
            .asSequence()
            .map(JsonNode::value)
            .flatten(recursively = false)
            .toList()

        fun raiseErrorDueToCountMismatch(): Nothing {
            error("If you use indexed hydration then you MUST follow a contract where the resolved nodes matches the size of the input arguments")
        }

        var resultIndex = 0
        return parentNodes
            .asSequence()
            // Ensure we don't go out of bounds
            .takeWhile { resultIndex <= resultValues.lastIndex }
            .map { parentNode ->
                val newValue = if (batchInputValueSource?.fieldDefinition?.type?.unwrapNonNull()?.isList == true) {
                    // The number of elements from the input source determines how many objects to take
                    val numValuesToTake = NadelBatchHydrationInputBuilder.getFieldResultValues(
                        valueSource = batchInputValueSource,
                        parentNode = parentNode,
                        aliasHelper = state.aliasHelper,
                    ).size

                    resultValues
                        .subListOrNull(resultIndex, toIndex = resultIndex + numValuesToTake)
                        .also {
                            // Null means index was out of bounds
                            it ?: raiseErrorDueToCountMismatch()
                            resultIndex += numValuesToTake
                        }
                } else {
                    resultValues[resultIndex++]
                }

                NadelResultInstruction.Set(
                    subjectPath = parentNode.resultPath + state.hydratedField.resultKey,
                    newValue = newValue,
                )
            }
            .toList()
            .also {
                if (resultIndex != resultValues.size) {
                    raiseErrorDueToCountMismatch()
                }
            }
    }

    private fun getHydrateInstructionsMatchingObjectId(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        resultNodes: List<JsonNode>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<NadelResultInstruction> {
        // Associate by does not need to be strict here
        val resultNodesByObjectId = resultNodes.asSequence()
            .filterNotNull()
            .map(JsonNode::value)
            .flatten(recursively = true)
            .mapNotNull {
                when (it) {
                    is AnyMap? -> it.asNullableJsonMap()
                    else -> error("Hydration actor result must be an object")
                }
            }
            .associateBy {
                // We don't want to show this in the overall result, so remove it here as we use it
                it.asMutable().remove(state.aliasHelper.getObjectIdentifierKey(matchStrategy.objectId))
            }

        val resultKeysToObjectIdOnHydrationParentNode = state.aliasHelper.getQueryPath(
            getPathToObjectIdentifierOnHydrationParentNode(instruction),
        )

        return parentNodes.map { parentNode ->
            val parentNodeIdentifierNodes = JsonNodeExtractor.getNodesAt(
                rootNode = parentNode,
                queryPath = resultKeysToObjectIdOnHydrationParentNode,
            )

            getHydrateInstructionsForNodeMatchingObjectId(
                executionBlueprint = executionBlueprint,
                state = state,
                parentNode = parentNode,
                parentNodeIdentifierNodes = parentNodeIdentifierNodes,
                resultNodesByObjectId = resultNodesByObjectId
            )
        }
    }

    private fun getHydrateInstructionsForNodeMatchingObjectId(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: State,
        parentNode: JsonNode,
        parentNodeIdentifierNodes: List<JsonNode>,
        resultNodesByObjectId: Map<Any?, JsonMap>,
    ): NadelResultInstruction {
        val hydratedFieldDef = NadelTransformUtil.getOverallFieldDef(
            overallField = state.hydratedField,
            parentNode = parentNode,
            service = state.hydratedFieldService,
            executionBlueprint = executionBlueprint,
            aliasHelper = state.aliasHelper,
        ) ?: error("Unable to find field definition for ${state.hydratedField.queryPath}")

        val newValue: Any? = if (hydratedFieldDef.type.unwrapNonNull().isList) {
            // Set to null if there were no identifier nodes
            if (parentNodeIdentifierNodes.all { it.value == null }) {
                null
            } else {
                parentNodeIdentifierNodes
                    .flatMap { parentNodeIdentifierNode ->
                        when (val id = parentNodeIdentifierNode.value) {
                            null -> emptySequence()
                            is AnyList -> id.asSequence().flatten(recursively = true)
                            else -> sequenceOf(id)
                        }
                    }
                    .map { id ->
                        resultNodesByObjectId[id]
                    }
                    .toList()
            }
        } else {
            parentNodeIdentifierNodes.emptyOrSingle()?.let { node ->
                resultNodesByObjectId[node.value]
            }
        }

        return NadelResultInstruction.Set(
            subjectPath = parentNode.resultPath + state.hydratedField.resultKey,
            newValue = newValue,
        )
    }

    private suspend fun executeBatches(
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<ServiceExecutionResult> {
        val actorQueries = NadelHydrationFieldsBuilder.makeBatchActorQueries(
            instruction = instruction,
            aliasHelper = state.aliasHelper,
            hydratedField = state.hydratedField,
            parentNodes = parentNodes,
        )

        return coroutineScope {
            actorQueries
                .map { actorQuery ->
                    async { // This async executes the batches in parallel i.e. executes hydration as Deferred/Future
                        engine.executeHydration(
                            service = instruction.actorService,
                            topLevelField = actorQuery,
                            pathToActorField = instruction.queryPathToActorField,
                            executionContext = state.executionContext,
                        )
                    }
                }
                .awaitAll()
        }
    }

    private fun getHydrationActorResultNodes(
        instruction: NadelBatchHydrationFieldInstruction,
        batches: List<ServiceExecutionResult>,
    ): List<JsonNode> {
        return batches
            .asSequence()
            .flatMap getNodes@{ batch ->
                JsonNodeExtractor.getNodesAt(
                    data = batch.data ?: return@getNodes emptySequence(),
                    queryPath = instruction.queryPathToActorField,
                ).asSequence()
            }
            .toList()
    }

    /**
     * For the following example
     *
     * ```
     * type Issue {
     *   details: IssueDetails
     *   owner: User @hydrated(from: ["issueOwner"], args: [
     *     {name: "issueId" valueFromField: ["details", "authorId"]}
     *   ])
     * }
     *
     * type IssueDetails {
     *   authorId: ID!
     * }
     * ```
     *
     * We are getting the path `["details", "authorId"]`
     */
    private fun getPathToObjectIdentifierOnHydrationParentNode(
        instruction: NadelBatchHydrationFieldInstruction,
    ): NadelQueryPath {
        return instruction
            .actorInputValueDefs
            .asSequence()
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationActorInputDef.ValueSource.FieldResultValue>()
            .single()
            .queryPathToField
    }
}
