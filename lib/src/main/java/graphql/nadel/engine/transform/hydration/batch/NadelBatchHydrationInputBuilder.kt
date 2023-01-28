package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelEngineContext
import graphql.nadel.engine.NadelEngineExecutionHooks
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.EffectFieldArgumentDef
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.engine.util.mapFrom
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLTypeUtil
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.TransformContext as BatchTransformContext

/**
 * README
 *
 * Please ensure that the batch arguments are ordered according to the input.
 * This is required for [NadelBatchHydrationMatchStrategy.MatchIndex].
 */
internal object NadelBatchHydrationInputBuilder {
    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    fun getInputValueBatches(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<Map<EffectFieldArgumentDef, NormalizedInputValue>> {
        val nonBatchArgs = getNonBatchInputValues(instruction)
        val batchArgs = getBatchInputValues(instruction, parentNodes)

        return batchArgs.map { nonBatchArgs + it }
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private fun getNonBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
    ): Map<EffectFieldArgumentDef, NormalizedInputValue> {
        return mapFrom(
            instruction.effectFieldArgDefs.mapNotNull { actorFieldArg ->
                when (val valueSource = actorFieldArg.valueSource) {
                    is EffectFieldArgumentDef.ValueSource.FromArgumentValue -> {
                        val argValue = hydrationCauseField.normalizedArguments[valueSource.argumentName]
                            ?: valueSource.defaultValue
                        if (argValue != null) {
                            actorFieldArg to argValue
                        } else {
                            null
                        }
                    }
                    // These are batch values, ignore them
                    is EffectFieldArgumentDef.ValueSource.FromResultValue -> null
                }
            },
        )
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private fun getBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<Pair<EffectFieldArgumentDef, NormalizedInputValue>> {
        val batchSize = instruction.batchSize

        val (batchInputDef, batchInputValueSource) = getBatchInputDef(instruction) ?: return emptyList()
        val actorBatchArgDef = instruction.effectFieldDef.getArgument(batchInputDef.name)

        val args = getFieldResultValues(batchInputValueSource, parentNodes)

        val partitionArgumentList = when (serviceExecutionHooks) {
            is NadelEngineExecutionHooks -> serviceExecutionHooks.partitionBatchHydrationArgumentList(args, instruction)
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
    ): Pair<EffectFieldArgumentDef, EffectFieldArgumentDef.ValueSource.FromResultValue>? {
        return instruction.effectFieldArgDefs
            .asSequence()
            .mapNotNull {
                when (val valueSource = it.valueSource) {
                    is EffectFieldArgumentDef.ValueSource.FromResultValue -> it to valueSource
                    else -> null
                }
            }
            .emptyOrSingle()
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    private fun getFieldResultValues(
        valueSource: EffectFieldArgumentDef.ValueSource.FromResultValue,
        parentNodes: List<JsonNode>,
    ): List<Any?> {
        return parentNodes.flatMap { parentNode ->
            getFieldResultValues(
                valueSource = valueSource,
                parentNode = parentNode,
                filterNull = true,
            )
        }
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    internal fun getFieldResultValues(
        valueSource: EffectFieldArgumentDef.ValueSource.FromResultValue,
        parentNode: JsonNode,
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
