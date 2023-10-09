package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.NadelEngineExecutionHooks
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.mapFrom
import graphql.nadel.hooks.ServiceExecutionHooks
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
        hooks: ServiceExecutionHooks,
    ): List<Map<NadelHydrationActorInputDef, NormalizedInputValue>> {
        val nonBatchArgs = getNonBatchInputValues(instruction, hydrationField)
        val batchArgs = getBatchInputValues(instruction, parentNodes, aliasHelper, hooks)

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
                        val argValue: NormalizedInputValue? = hydrationField.normalizedArguments[valueSource.argumentName]
                            ?: valueSource.defaultValue
                        if (argValue != null) {
                            actorFieldArg to argValue
                        } else {
                            null
                        }
                    }
                    // These are batch values, ignore them
                    is NadelHydrationActorInputDef.ValueSource.FieldResultValue -> null
                    is NadelHydrationActorInputDef.ValueSource.StaticValue -> {
                        val staticValue: NormalizedInputValue = makeNormalizedInputValue(
                            type = actorFieldArg.actorArgumentDef.type,
                            value = valueSource.value,
                        )
                        actorFieldArg to staticValue
                    }
                }
            },
        )
    }

    private fun getBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        aliasHelper: NadelAliasHelper,
        hooks: ServiceExecutionHooks,
    ): List<Pair<NadelHydrationActorInputDef, NormalizedInputValue>> {
        val batchSize = instruction.batchSize

        val (batchInputDef, batchInputValueSource) = getBatchInputDef(instruction) ?: return emptyList()
        val actorBatchArgDef = instruction.actorFieldDef.getArgument(batchInputDef.name)

        val args = getFieldResultValues(batchInputValueSource, parentNodes, aliasHelper)

        val partitionArgumentList = when (hooks) {
            is NadelEngineExecutionHooks -> hooks.partitionBatchHydrationArgumentList(args, instruction)
            else -> listOf(args)
        }

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

    private fun getFieldResultValues(
        valueSource: NadelHydrationActorInputDef.ValueSource.FieldResultValue,
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
        valueSource: NadelHydrationActorInputDef.ValueSource.FieldResultValue,
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
