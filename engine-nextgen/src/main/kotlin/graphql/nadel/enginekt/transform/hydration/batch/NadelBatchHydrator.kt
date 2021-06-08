package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInput
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.getInstructionForNode
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.normalized.NormalizedInputValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class NadelBatchHydrator(
    private val engine: NextgenEngine,
) {
    suspend fun hydrate(
        state: State,
        executionPlan: NadelExecutionPlan,
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val parentNodesByInstruction: Map<NadelBatchHydrationFieldInstruction, List<JsonNode>> = parentNodes
            .mapNotNull { parentNode ->
                val instruction = state.instructions.getInstructionForNode(
                    executionPlan = executionPlan,
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
            hydrate(state, instruction, parentNodes)
        }
    }

    private suspend fun hydrate(
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val argBatches = NadelBatchHydrationInputBuilder.getInputValueBatches(
            aliasHelper = state.aliasHelper,
            instruction = instruction,
            hydrationField = state.field,
            parentNodes = parentNodes,
        )

        val batches: List<Deferred<ServiceExecutionResult>> = executeBatchesAsync(state, instruction, argBatches)
        val resultsByObjectId = awaitBatchesThenAssociateKeys(instruction, batches)
        val resultKeysToObjectIdOnHydrationParentNode = state.aliasHelper.mapQueryPathRespectingResultKey(
            getPathToObjectIdentifierOnHydrationParentNode(instruction),
        )

        return parentNodes.mapNotNull { parentNode ->
            val parentNodeIdentifierNode = JsonNodeExtractor.getNodesAt(
                rootNode = parentNode,
                queryPath = resultKeysToObjectIdOnHydrationParentNode,
            ).emptyOrSingle()

            when (parentNodeIdentifierNode) {
                null -> null
                else -> NadelResultInstruction.Set(
                    subjectPath = parentNode.resultPath + state.field.resultKey,
                    newValue = resultsByObjectId[parentNodeIdentifierNode.value],
                )
            }
        }
    }

    private suspend fun executeBatchesAsync(
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        argBatches: List<Map<NadelHydrationActorInput, NormalizedInputValue>>,
    ): List<Deferred<ServiceExecutionResult>> {
        return argBatches
            .map { argBatch ->
                val hydrationQuery = NadelHydrationFieldsBuilder.getQuery(
                    instruction = instruction,
                    hydrationField = state.field,
                    fieldArguments = argBatch.mapKeys { (argument) -> argument.name },
                )

                coroutineScope {
                    async {
                        engine.executeHydration(
                            service = instruction.actorService,
                            topLevelField = hydrationQuery,
                            pathToSourceField = instruction.actorFieldQueryPath,
                            executionContext = state.executionContext,
                        )
                    }
                }
            }
    }

    private suspend fun awaitBatchesThenAssociateKeys(
        instruction: NadelBatchHydrationFieldInstruction,
        batches: List<Deferred<ServiceExecutionResult>>,
    ): Map<Any?, JsonMap> {
        return batches
            .awaitAll()
            .flatMap { batch ->
                val nodes = JsonNodeExtractor.getNodesAt(
                    data = batch.data,
                    queryPath = instruction.actorFieldQueryPath,
                    flatten = true,
                )

                // Associate by does not need to be strict here
                nodes
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
            .associateBy { nodeValue ->
                when (val matchStrategy = instruction.batchHydrationMatchStrategy) {
                    is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> nodeValue[matchStrategy.objectId]
                    is NadelBatchHydrationMatchStrategy.MatchIndex -> TODO("no-op")
                }
            }
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
            .filterIsInstance<NadelHydrationArgumentValueSource.FieldResultValue>()
            .single()
            .queryPathToField
    }
}
