package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelEngineContext
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelEngineExecutionHooks
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationByIndex.Companion.getHydrateInstructionsMatchingIndex
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationByObjectId.getHydrateInstructionsMatchingObjectId
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationByObjectId.getHydrateInstructionsMatchingObjectIds
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.TransformContext as BatchTransformContext

internal class NadelBatchHydrator(
    private val engine: NextgenEngine,
) {
    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    suspend fun hydrate(
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val parentNodesByInstruction: Map<NadelBatchHydrationFieldInstruction?, List<JsonNode>> = parentNodes
            .mapNotNull { parentNode ->
                val instructions = instructionsByObjectTypeNames.getInstructionsForNode(
                    executionBlueprint = executionBlueprint,
                    service = hydrationCauseService,
                    aliasHelper = aliasHelper,
                    parentNode = parentNode,
                )

                // Becomes Pair<JsonNode, Instruction?>
                when {
                    instructions.isEmpty() -> null
                    instructions.size == 1 -> parentNode to instructions.single()
                    else -> parentNode to getHydrationInstruction(instructions, parentNode)
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
                                key = NadelResultKey(hydrationCauseField.resultKey),
                                newValue = null,
                            )
                        }

                        else -> hydrate(instruction, parentNodes)
                    }
                }
            }
        }

        return jobs.awaitAll().flatten()
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private suspend fun hydrate(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val batches: List<ServiceExecutionResult> = executeBatches(
            instruction = instruction,
            parentNodes = parentNodes
        )

        return when (val matchStrategy = instruction.batchHydrationMatchStrategy) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> getHydrateInstructionsMatchingIndex(
                instruction = instruction,
                parentNodes = parentNodes,
                batches = batches,
            )

            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> getHydrateInstructionsMatchingObjectId(
                instruction = instruction,
                parentNodes = parentNodes,
                batches = batches,
                matchStrategy = matchStrategy,
            )

            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> getHydrateInstructionsMatchingObjectIds(
                instruction = instruction,
                parentNodes = parentNodes,
                batches = batches,
                matchStrategy = matchStrategy,
            )
        } + getInstructionsToAddErrors(batches)
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private suspend fun executeBatches(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<ServiceExecutionResult> {
        val actorQueries = NadelHydrationFieldsBuilder.makeBatchEffectQueries(
            instruction = instruction,
            parentNodes = parentNodes,
        )

        val engineContext = this@NadelEngineContext
        val executionContext = this@NadelExecutionContext

        return coroutineScope {
            actorQueries
                .map { actorQuery ->
                    async { // This async executes the batches in parallel i.e. executes hydration as Deferred/Future
                        val hydrationSourceService = executionBlueprint.getServiceOwning(instruction.location)!!
                        val hydrationActorField =
                            FieldCoordinates.coordinates(instruction.effectFieldContainer, instruction.effectFieldDef)

                        val serviceHydrationDetails = ServiceExecutionHydrationDetails(
                            timeout = instruction.timeout,
                            batchSize = instruction.batchSize,
                            hydrationCauseService = hydrationSourceService,
                            hydrationCauseField = instruction.location,
                            hydrationEffectField = hydrationActorField
                        )

                        engine.executeTopLevelField(
                            engineContext,
                            executionContext,
                            service = instruction.effectService,
                            topLevelField = actorQuery,
                            serviceHydrationDetails = serviceHydrationDetails,
                        )
                    }
                }
                .awaitAll()
        }
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private fun getHydrationInstruction(
        instructions: List<NadelBatchHydrationFieldInstruction>,
        parentNode: JsonNode,
    ): NadelBatchHydrationFieldInstruction? {
        if (serviceExecutionHooks !is NadelEngineExecutionHooks) {
            error(
                "Cannot decide which hydration instruction should be used. " +
                    "Provided ServiceExecutionHooks has to be of type NadelEngineExecutionHooks"
            )
        }

        return serviceExecutionHooks.getHydrationInstruction(
            instructions,
            parentNode,
            aliasHelper,
            userContext,
        )
    }
}
