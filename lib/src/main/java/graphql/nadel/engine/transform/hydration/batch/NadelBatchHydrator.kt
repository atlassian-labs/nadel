package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationByIndex.Companion.getHydrateInstructionsMatchingIndex
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationByObjectId.getHydrateInstructionsMatchingObjectId
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationByObjectId.getHydrateInstructionsMatchingObjectIds
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.schema.FieldCoordinates
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
                                subject = it,
                                key = NadelResultKey(state.hydratedField.resultKey),
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
            parentNodes = parentNodes
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
            hooks = state.executionContext.hooks,
            userContext = state.executionContext.userContext,
        )

        return coroutineScope {
            actorQueries
                .map { actorQuery ->
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
                        engine.executeHydration(
                            service = instruction.actorService,
                            topLevelField = actorQuery,
                            executionContext = state.executionContext,
                            hydrationDetails = serviceHydrationDetails,
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
        return state.executionContext.hooks.getHydrationInstruction(
            instructions,
            parentNode,
            state.aliasHelper,
            state.executionContext.userContext
        )
    }
}
