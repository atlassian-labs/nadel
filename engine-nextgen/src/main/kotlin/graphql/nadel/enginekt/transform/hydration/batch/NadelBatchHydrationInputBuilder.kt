package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.hydration.NadelHydrationInputBuilder.valueToAstValue
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLTypeUtil

/**
 * README
 *
 * Please ensure that the batch arguments are ordered according to the input.
 * This is required for [NadelBatchHydrationMatchStrategy.MatchIndex].
 */
internal object NadelBatchHydrationInputBuilder {
    fun getInputValueBatches(
        aliasHelper: NadelAliasHelper,
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: ExecutableNormalizedField,
        parentNodes: List<JsonNode>,
    ): List<Map<NadelHydrationActorInputDef, NormalizedInputValue>> {
        val nonBatchArgs = getNonBatchInputValues(instruction, hydrationField)
        val batchArgs = getBatchInputValues(instruction, parentNodes, aliasHelper)

        return batchArgs.map { nonBatchArgs + it }
    }

    private fun getNonBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: ExecutableNormalizedField,
    ): Map<NadelHydrationActorInputDef, NormalizedInputValue> {
        return mapFrom(
            instruction.actorInputValueDefs.mapNotNull { actorFieldArg ->
                when (val valueSource = actorFieldArg.valueSource) {
                    is NadelHydrationActorInputDef.ValueSource.ArgumentValue -> {
                        when (val argValue = hydrationField.normalizedArguments[valueSource.argumentName]) {
                            null -> null
                            else -> actorFieldArg to argValue
                        }
                    }
                    // These are batch values, ignore them
                    is NadelHydrationActorInputDef.ValueSource.FieldResultValue -> null
                }
            },
        )
    }

    private fun getBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        aliasHelper: NadelAliasHelper,
    ): List<Pair<NadelHydrationActorInputDef, NormalizedInputValue>> {
        val batchSize = instruction.batchSize

        val (batchInputDef, batchInputValueSource) = getBatchInputDef(instruction) ?: return emptyList()
        val actorBatchArgDef = instruction.actorFieldDef.getArgument(batchInputDef.name)

        return getFieldResultValues(batchInputValueSource, parentNodes, aliasHelper)
            .chunked(size = batchSize)
            .map { chunk ->
                batchInputDef to NormalizedInputValue(
                    GraphQLTypeUtil.simplePrint(actorBatchArgDef.type),
                    valueToAstValue(chunk),
                )
            }
    }

    /**
     * Get the input def that is collated together to form the batch input.
     *
     * e.g. for a schema
     *
     * ```graphql
     * type User {
     *   friendId: [ID]
     *   friend(acquaintances: Boolean! = false): User @hydrated(
     *     from: "usersByIds",
     *     arguments: [
     *       {name: "userIds", valueFromField: "friendId"}
     *       {name: "acquaintances", valueFromArgument: "acquaintances"}
     *     ],
     *   )
     * }
     * ```
     *
     * then the input def would be the `userIds`.
     */
    internal fun getBatchInputDef(
        instruction: NadelBatchHydrationFieldInstruction,
    ): Pair<NadelHydrationActorInputDef, NadelHydrationActorInputDef.ValueSource.FieldResultValue>? {
        return instruction.actorInputValueDefs
            .asSequence()
            .mapNotNull {
                when (val valueSource = it.valueSource) {
                    is NadelHydrationActorInputDef.ValueSource.FieldResultValue -> it to valueSource
                    else -> null
                }
            }
            .emptyOrSingle()
    }

    internal fun getFieldResultValues(
        valueSource: NadelHydrationActorInputDef.ValueSource.FieldResultValue,
        parentNodes: List<JsonNode>,
        aliasHelper: NadelAliasHelper,
    ): List<Any?> {
        return parentNodes.flatMap { parentNode ->
            getFieldResultValues(
                valueSource = valueSource,
                parentNode = parentNode,
                aliasHelper = aliasHelper
            )
        }
    }

    internal fun getFieldResultValues(
        valueSource: NadelHydrationActorInputDef.ValueSource.FieldResultValue,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
    ): List<Any?> {
        val nodes = JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.getQueryPath(valueSource.queryPathToField),
            flatten = true,
        )

        return nodes.asSequence()
            .map { it.value }
            .flatten(recursively = true)
            .filterNotNull()
            .toList()
    }
}
