package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.javaValueToAstValue
import graphql.nadel.enginekt.util.mapFrom
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
        hooks: ServiceExecutionHooks,
    ): List<Pair<NadelHydrationActorInputDef, NormalizedInputValue>> {
        val batchSize = instruction.batchSize

        val (batchInputDef, batchInputValueSource) = getBatchInputDef(instruction) ?: return emptyList()
        val actorBatchArgDef = instruction.actorFieldDef.getArgument(batchInputDef.name)

        val args = getFieldResultValues(batchInputValueSource, parentNodes, aliasHelper)

        val partitionArgumentList = when (hooks) {
            is NadelEngineExecutionHooks -> hooks.partitionArgumentList(args, instruction)
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
    ): List<Any> {
        return parentNodes.flatMap { parentNode ->
            getFieldResultValues(
                valueSource = valueSource,
                parentNode = parentNode,
                aliasHelper = aliasHelper
            ).filterNotNull()
        }
    }

    internal fun getFieldResultValues(
        valueSource: NadelHydrationActorInputDef.ValueSource.FieldResultValue,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper
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
            .toList()
    }
}
