package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInput
import graphql.nadel.enginekt.transform.NadelTransformUtil
import graphql.nadel.enginekt.transform.getInstructionForNode
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.asMutable
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.getField
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.unwrapNonNull
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

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
        val deferredBatches: List<Deferred<ServiceExecutionResult>> = executeBatchesAsync(
            state = state,
            instruction = instruction,
            parentNodes = parentNodes
        )
        val resultNodes: List<JsonMap> = getHydrationActorResultNodes(instruction, deferredBatches)

        return when (val matchStrategy = instruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> getHydrateInstructionsMatchingIndex(
                state = state,
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
        }
    }

    private fun getHydrateInstructionsMatchingIndex(
        state: State,
        parentNodes: List<JsonNode>,
        resultNodes: List<JsonMap>,
    ): List<NadelResultInstruction> {
        require(resultNodes.size == parentNodes.size) { "Could not hydrate by index: illegal element count" }

        return parentNodes.mapIndexed { index, parentNode ->
            NadelResultInstruction.Set(
                subjectPath = parentNode.resultPath + state.hydratedField.resultKey,
                newValue = resultNodes[index],
            )
        }
    }

    private fun getHydrateInstructionsMatchingObjectId(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        resultNodes: List<JsonMap>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<NadelResultInstruction> {
        val resultNodesByObjectId = resultNodes.associateBy {
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

            val overallTypeName = NadelTransformUtil.getOverallTypeNameOfNode(
                executionBlueprint = executionBlueprint,
                service = state.hydratedFieldService,
                aliasHelper = state.aliasHelper,
                node = parentNode,
            )
            val hydratedFieldCoordinates = makeFieldCoordinates(overallTypeName, state.hydratedField.name)
            val hydratedFieldDef = executionBlueprint.schema.getField(hydratedFieldCoordinates)
                ?: error("Unable to find field at $hydratedFieldCoordinates")

            val newValue: Any? = if (hydratedFieldDef.type.unwrapNonNull().isList) {
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
            } else {
                parentNodeIdentifierNodes.emptyOrSingle()?.let { node ->
                    resultNodesByObjectId[node.value]
                }
            }


            NadelResultInstruction.Set(
                subjectPath = parentNode.resultPath + state.hydratedField.resultKey,
                newValue = newValue,
            )
        }
    }

    private suspend fun executeBatchesAsync(
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<Deferred<ServiceExecutionResult>> {
        val actorQueries = NadelHydrationFieldsBuilder.makeActorQueries(
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
        }
    }

    private suspend fun getHydrationActorResultNodes(
        instruction: NadelBatchHydrationFieldInstruction,
        batches: List<Deferred<ServiceExecutionResult>>,
    ): List<JsonMap> {
        return batches
            .awaitAll()
            .asSequence()
            .flatMap { batch ->
                val nodes = JsonNodeExtractor.getNodesAt(
                    data = batch.data,
                    queryPath = instruction.queryPathToActorField,
                    flatten = true,
                )

                // Associate by does not need to be strict here
                nodes
                    .asSequence()
                    .mapNotNull { node ->
                        when (val nodeValue = node.value) {
                            is AnyMap -> nodeValue.let {
                                @Suppress("UNCHECKED_CAST")
                                it as JsonMap
                            }
                            else -> null
                        }
                    }
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
    ): QueryPath {
        return instruction
            .actorInputValues
            .asSequence()
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationActorInput.ValueSource.FieldResultValue>()
            .single()
            .queryPathToField
    }
}
