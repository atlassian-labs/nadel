package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder.getBatchInputDef
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.listOfNulls
import graphql.nadel.engine.util.unwrapNonNull
import graphql.normalized.ExecutableNormalizedField

internal class NadelBatchHydrationByIndex private constructor(
    private val fieldToHydrate: ExecutableNormalizedField,
    private val aliasHelper: NadelAliasHelper,
    private val instruction: NadelBatchHydrationFieldInstruction,
    private val parentNodes: List<JsonNode>,
    private val batches: List<ServiceExecutionResult>,
) {
    companion object {
        fun getHydrateInstructionsMatchingIndex(
            state: NadelBatchHydrationTransform.State,
            instruction: NadelBatchHydrationFieldInstruction,
            parentNodes: List<JsonNode>,
            batches: List<ServiceExecutionResult>,
        ): List<NadelResultInstruction> {
            return NadelBatchHydrationByIndex(
                fieldToHydrate = state.hydratedField,
                aliasHelper = state.aliasHelper,
                instruction = instruction,
                parentNodes = parentNodes,
                batches = batches,
            ).getHydrateInstructions()
        }
    }

    fun getHydrateInstructions(): List<NadelResultInstruction> {
        val isManyInputNodesToParentNodes = isManyInputNodesToParentNodes(instruction)
        val chunker = Chunker(instruction, batches)

        return parentNodes
            .map { parentNode ->
                val inputValues = getInputValues(parentNode)

                NadelResultInstruction.Set(
                    subject = parentNode,
                    key = NadelResultKey(fieldToHydrate.resultKey),
                    newValue = JsonNode(
                        if (isManyInputNodesToParentNodes) {
                            chunker.take(inputValues)
                        } else {
                            chunker.takeOne(
                                inputValue = inputValues.emptyOrSingle(),
                            )
                        },
                    ),
                )
            }
    }

    private fun getInputValues(
        parentNode: JsonNode,
    ): List<Any?> {
        val batchInputValueSource = getBatchInputValueSource(instruction)

        return NadelBatchHydrationInputBuilder.getFieldResultValues(
            batchInputValueSource,
            parentNode,
            aliasHelper,
            filterNull = false, // We want nulls
        )
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
        return getBatchInputValueSource(instruction)
            .fieldDefinition
            .type
            .unwrapNonNull()
            .isList
    }

    private fun getBatchInputValueSource(
        instruction: NadelBatchHydrationFieldInstruction,
    ): NadelHydrationActorInputDef.ValueSource.FieldResultValue {
        val (_, batchInputValueSource) = getBatchInputDef(instruction)
            ?: error("Batch hydration is missing batch input arg") // TODO: we should bake this into the instruction

        return batchInputValueSource
    }

    private class Chunker(
        private val instruction: NadelBatchHydrationFieldInstruction,
        batches: List<ServiceExecutionResult>,
    ) {
        private val values: List<Any?> = getValues(batches)
        private var cursorIndex = 0

        fun take(inputValues: List<Any?>): List<Any?> {
            return inputValues.map(::takeOne)
        }

        fun takeOne(inputValue: Any?): Any? {
            if (inputValue == null) {
                return null
            }

            if (cursorIndex > values.lastIndex) {
                badCount()
            }

            return values[cursorIndex++]
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
