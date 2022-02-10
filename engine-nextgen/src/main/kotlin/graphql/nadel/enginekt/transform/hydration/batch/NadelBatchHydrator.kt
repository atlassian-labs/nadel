package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.transform.getInstructionsForNode
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationByIndex.Companion.getHydrateInstructionsMatchingIndex
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationByObjectId.getHydrateInstructionsMatchingObjectId
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationByObjectId.getHydrateInstructionsMatchingObjectIds
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import kotlinx.coroutines.Deferred
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
        val parentNodesByInstruction: Map<NadelBatchHydrationFieldInstruction?, List<JsonNode>> = parentNodes
            .mapNotNull { parentNode ->
                val instructions = state.instructionsByObjectTypeNames.getInstructionsForNode(
                    executionBlueprint = executionBlueprint,
                    service = state.hydratedFieldService,
                    aliasHelper = state.aliasHelper,
                    parentNode = parentNode,
                )

                // Becomes Pair<JsonNode, Instruction?>
                when {
                    instructions.isEmpty() -> null
                    instructions.size == 1 -> parentNode to instructions.single()
                    else -> parentNode to getHydrationInstruction(state, instructions, parentNode)
                }
            }
            // Becomes Map<Instruction?, List<Pair<JsonNode, Instruction?>>>
            .groupBy { pair ->
                pair.second
            }
            // Changes List<Pair<JsonNode, Instruction?>> to List<JsonNode>
            .mapValues { pairs ->
                pairs.value.map { pair -> pair.first }
            }

        val jobs: List<Deferred<List<NadelResultInstruction>>> = coroutineScope {
            parentNodesByInstruction.map { (instruction, parentNodes) ->
                async {
                    when (instruction) {
                        null -> parentNodes.map {
                            NadelResultInstruction.Set(
                                it.resultPath + state.hydratedField.fieldName,
                                newValue = null,
                            )
                        }
                        else -> hydrate(executionBlueprint, state, instruction, parentNodes)
                    }
                }
            }
        }

        return jobs.awaitAll().flatten()
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

        return when (val matchStrategy = instruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> getHydrateInstructionsMatchingIndex(
                state = state,
                instruction = instruction,
                parentNodes = parentNodes,
                batches = batches,
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> getHydrateInstructionsMatchingObjectId(
                executionBlueprint = executionBlueprint,
                state = state,
                instruction = instruction,
                parentNodes = parentNodes,
                batches = batches,
                matchStrategy = matchStrategy,
            )
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> getHydrateInstructionsMatchingObjectIds(
                executionBlueprint = executionBlueprint,
                state = state,
                instruction = instruction,
                parentNodes = parentNodes,
                batches = batches,
                matchStrategy = matchStrategy,
            )
        } + getInstructionsToAddErrors(batches)
    }

    private suspend fun executeBatches(
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<ServiceExecutionResult> {
        val executionBlueprint = state.executionBlueprint
        val actorQueries = NadelHydrationFieldsBuilder.makeBatchActorQueries(
            executionBlueprint = executionBlueprint,
            instruction = instruction,
            aliasHelper = state.aliasHelper,
            hydratedField = state.hydratedField,
            parentNodes = parentNodes,
            hooks = state.executionContext.hooks
        )

        return coroutineScope {
            actorQueries
                .map { actorQuery ->
                    async { // This async executes the batches in parallel i.e. executes hydration as Deferred/Future
                        val hydrationSourceService =
                            executionBlueprint.getServiceOwning(instruction.location)
                        engine.executeHydration(
                            service = instruction.actorService,
                            topLevelField = actorQuery,
                            pathToActorField = instruction.queryPathToActorField,
                            executionContext = state.executionContext,
                            serviceHydrationDetails = ServiceExecutionHydrationDetails(
                                instruction.timeout,
                                instruction.batchSize,
                                hydrationSourceService,
                                instruction.location
                            )
                        )
                    }
                }
                .awaitAll()
        }
    }

    private fun getHydrationInstruction(
        state: State,
        instructions: List<NadelBatchHydrationFieldInstruction>,
        parentNode: JsonNode,
    ): NadelBatchHydrationFieldInstruction? {
        if (state.executionContext.hooks !is NadelEngineExecutionHooks) {
            error(
                "Cannot decide which hydration instruction should be used. " +
                    "Provided ServiceExecutionHooks has to be of type NadelEngineExecutionHooks"
            )
        }
        return state.executionContext.hooks.getHydrationInstruction(
            instructions,
            parentNode,
            state.aliasHelper
        )
    }
}
