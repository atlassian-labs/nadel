package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getNumberOfInputNodes
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationInputBuilder.getBatchInputDef
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.listOfNulls
import graphql.nadel.enginekt.util.subListOrNull
import graphql.nadel.enginekt.util.unwrapNonNull

internal object NadelBatchHydrationByIndex {
    fun getHydrateInstructionsMatchingIndex(
        state: NadelBatchHydrationTransform.State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
    ): List<NadelResultInstruction> {
        val isManyInputNodesToParentNodes = isManyInputNodesToParentNodes(instruction)
        val chunker = Chunker(instruction, batches)

        return parentNodes
            .map { parentNode ->
                NadelResultInstruction.Set(
                    subjectPath = parentNode.resultPath + state.hydratedField.resultKey,
                    newValue = if (isManyInputNodesToParentNodes) {
                        chunker.take(
                            n = getNumberOfInputNodes(
                                aliasHelper = state.aliasHelper,
                                instruction = instruction,
                                parentNode = parentNode,
                            ),
                        )
                    } else {
                        chunker.take()
                    },
                )
            }
    }

    /**
     * This determines whether the parent node definition had a List for the input batch arg e.g.
     *
     * ```graphql
     * type Issue {
     *   userDetails: [JSON]
     *   users: [User] @hydrated(
     *      from: "usersByDetails"
     *      arguments: [
     *        {name: "details" valueFromField: "userDetails"}
     *      ]
     *   )
     * }
     * type Query {
     *   userByDetails(details: [JSON]): [User]
     * }
     * ```
     *
     * i.e. if `userDetails` is a List then hydration needs to take multiple values to set at `Issue.users`
     */
    private fun isManyInputNodesToParentNodes(instruction: NadelBatchHydrationFieldInstruction): Boolean {
        val (_, batchInputValueSource) = getBatchInputDef(instruction)
            ?: error("Batch hydration is missing batch input arg") // TODO: we should bake this into the instruction
        return batchInputValueSource.fieldDefinition.type.unwrapNonNull().isList
    }

    private class Chunker(
        private val instruction: NadelBatchHydrationFieldInstruction,
        batches: List<ServiceExecutionResult>,
    ) {
        private val values: List<Any?> = getValues(batches)
        private var cursorIndex = 0

        fun take(): Any? {
            if (cursorIndex > values.lastIndex) {
                badCount()
            }

            return values[cursorIndex++]
        }

        fun take(n: Int): List<Any?> {
            val sublistStart = cursorIndex
            val subListEnd = sublistStart + n
            cursorIndex += n
            return values.subListOrNull(sublistStart, subListEnd) ?: badCount()
        }

        private fun getValues(batches: List<ServiceExecutionResult>): List<Any?> {
            return batches
                .flatMapIndexed { index, batch ->
                    val actorNode = NadelHydrationUtil.getHydrationActorNode(instruction, batch)
                    when (val value = actorNode?.value) {
                        null -> listOfNulls(instruction.batchSize)
                        is AnyList -> value.also {
                            // Ensure API returned correct number of elements
                            if (index != batches.lastIndex && value.size != instruction.batchSize) {
                                badCount()
                            }
                        }
                        else -> error("Unsupported batch result type")
                    }
                }
        }

        private fun badCount(): Nothing {
            error("If you use indexed hydration then you MUST follow a contract where the resolved nodes matches the size of the input arguments")
        }
    }
}
