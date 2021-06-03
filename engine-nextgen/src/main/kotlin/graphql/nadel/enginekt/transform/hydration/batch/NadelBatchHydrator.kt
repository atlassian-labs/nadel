package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform.State
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
    private val logic: NadelBatchHydrationLogic,
) {
    suspend fun hydrate(
        state: State,
        executionPlan: NadelExecutionPlan,
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        val parentNodesByInstruction: Map<NadelBatchHydrationFieldInstruction, List<JsonNode>> = parentNodes
            .mapNotNull {
                when (val instruction = logic.getMatchingInstruction(parentNode = it, state, executionPlan)) {
                    null -> null
                    else -> it to instruction // Becomes Pair<JsonNode, Instruction>
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
        val argBatches = NadelBatchArgumentsBuilder.getArgumentBatches(
            artificialFields = state.artificialFields,
            instruction = instruction,
            hydrationField = state.field,
            parentNodes = parentNodes,
        )

        val batches: List<Deferred<ServiceExecutionResult>> = executeBatchesAsync(state, instruction, argBatches)
        val results = awaitBatchesThenAssociateKeys(instruction, batches)
        val resultKeysToObjectIdOnHydrationParentNode = state.artificialFields.mapPathToResultKeys(
            getPathToObjectIdentifierOnHydrationParentNode(instruction),
        )

        return parentNodes.mapNotNull { parentNode ->
            val parentNodeIdentifierNode = JsonNodeExtractor.getNodesAt(
                rootNode = parentNode,
                queryResultKeyPath = resultKeysToObjectIdOnHydrationParentNode,
            ).emptyOrSingle()

            when (parentNodeIdentifierNode) {
                null -> null
                else -> NadelResultInstruction.Set(
                    subjectPath = parentNode.path + state.field.resultKey,
                    newValue = results[parentNodeIdentifierNode.value],
                )
            }
        }
    }

    private suspend fun executeBatchesAsync(
        state: State,
        instruction: NadelBatchHydrationFieldInstruction,
        argBatches: List<Map<NadelHydrationArgument, NormalizedInputValue>>,
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
                            service = instruction.sourceService,
                            topLevelField = hydrationQuery,
                            pathToSourceField = instruction.pathToSourceField,
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
                    queryResultKeyPath = instruction.pathToSourceField,
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
    ): List<String> {
        return instruction
            .sourceFieldArguments
            .asSequence()
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationArgumentValueSource.FieldValue>()
            .single()
            .pathToField
    }
}
