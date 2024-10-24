package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.mapFrom
import graphql.nadel.hooks.NadelExecutionHooks
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
        virtualField: ExecutableNormalizedField,
        parentNodes: List<JsonNode>,
        hooks: NadelExecutionHooks,
        userContext: Any?,
    ): List<Map<NadelHydrationArgument, NormalizedInputValue>> {
        val nonBatchArgs = getNonBatchInputValues(instruction, virtualField)
        val batchArgs = getBatchInputValues(instruction, parentNodes, aliasHelper, hooks, userContext)

        return batchArgs.map { nonBatchArgs + it }
    }

    internal fun getNonBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        virtualField: ExecutableNormalizedField,
    ): Map<NadelHydrationArgument, NormalizedInputValue> {
        return mapFrom(
            instruction.backingFieldArguments.mapNotNull { backingFieldArg ->
                when (val valueSource = backingFieldArg.valueSource) {
                    is NadelHydrationArgument.ValueSource.ArgumentValue -> {
                        val argValue: NormalizedInputValue? =
                            virtualField.normalizedArguments[valueSource.argumentName]
                                ?: valueSource.defaultValue
                        if (argValue != null) {
                            backingFieldArg to argValue
                        } else {
                            null
                        }
                    }
                    // These are batch values, ignore them
                    is NadelHydrationArgument.ValueSource.FieldResultValue -> null
                    is NadelHydrationArgument.ValueSource.StaticValue -> {
                        val staticValue: NormalizedInputValue = makeNormalizedInputValue(
                            type = backingFieldArg.backingArgumentDef.type,
                            value = valueSource.value,
                        )
                        backingFieldArg to staticValue
                    }
                }
            },
        )
    }

    private fun getBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        aliasHelper: NadelAliasHelper,
        hooks: NadelExecutionHooks,
        userContext: Any?,
    ): List<Pair<NadelHydrationArgument, NormalizedInputValue>> {
        val batchSize = instruction.batchSize

        val (batchInputDef, batchInputValueSource) = getBatchInputDef(instruction) ?: return emptyList()
        val actorBatchArgDef = instruction.backingFieldDef.getArgument(batchInputDef.name)

        val args = getFieldResultValues(batchInputValueSource, parentNodes, aliasHelper)

        val partitionArgumentList = hooks.partitionBatchHydrationArgumentList(args, instruction, userContext)

        return partitionArgumentList.flatMap { it.chunked(size = batchSize) }
            .map { chunk ->
                batchInputDef to NormalizedInputValue(
                    GraphQLTypeUtil.simplePrint(actorBatchArgDef.type),
                    javaValueToAstValue(chunk),
                )
            }
    }

    /**
     * TODO: this should really be baked into the [instruction] and also be mandatory…
     *
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
    ): Pair<NadelHydrationArgument, NadelHydrationArgument.ValueSource.FieldResultValue>? {
        return instruction.backingFieldArguments
            .asSequence()
            .mapNotNull {
                when (val valueSource = it.valueSource) {
                    is NadelHydrationArgument.ValueSource.FieldResultValue -> it to valueSource
                    else -> null
                }
            }
            .emptyOrSingle()
    }

    private fun getFieldResultValues(
        valueSource: NadelHydrationArgument.ValueSource.FieldResultValue,
        parentNodes: List<JsonNode>,
        aliasHelper: NadelAliasHelper,
    ): List<Any?> {
        return parentNodes.flatMap { parentNode ->
            getFieldResultValues(
                valueSource = valueSource,
                parentNode = parentNode,
                aliasHelper = aliasHelper,
                filterNull = true,
            )
        }
    }

    internal fun getFieldResultValues(
        valueSource: NadelHydrationArgument.ValueSource.FieldResultValue,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
        filterNull: Boolean,
    ): List<Any?> {
        val nodes = JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.getQueryPath(valueSource.queryPathToField),
            flatten = true,
        )

        return nodes
            .asSequence()
            .map { it.value }
            .flatten(recursively = true)
            .let {
                if (filterNull) {
                    it.filterNotNull()
                } else {
                    it
                }
            }
            .toList()
    }
}
