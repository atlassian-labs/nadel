package graphql.nadel.engine.transform.hydration

import graphql.language.NullValue
import graphql.language.Value
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.toMapStrictly
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLTypeUtil

internal class NadelHydrationInputBuilder private constructor(
    private val instruction: NadelHydrationFieldInstruction,
    private val aliasHelper: NadelAliasHelper,
    private val virtualField: ExecutableNormalizedField,
    private val parentNode: JsonNode,
) {
    companion object {
        fun getInputValues(
            instruction: NadelHydrationFieldInstruction,
            aliasHelper: NadelAliasHelper,
            virtualField: ExecutableNormalizedField,
            parentNode: JsonNode,
        ): List<Map<String, NormalizedInputValue>> {
            return NadelHydrationInputBuilder(instruction, aliasHelper, virtualField, parentNode)
                .build()
        }
    }

    private fun build(): List<Map<String, NormalizedInputValue>> {
        return when (val hydrationStrategy = instruction.hydrationStrategy) {
            is NadelHydrationStrategy.OneToOne -> makeOneToOneArgs()
            is NadelHydrationStrategy.ManyToOne -> makeManyToOneArgs(hydrationStrategy)
        }
    }

    private fun makeOneToOneArgs(): List<Map<String, NormalizedInputValue>> {
        return listOfNotNull(
            makeInputMap().takeIf(::isInputMapValid),
        )
    }

    private fun makeManyToOneArgs(
        hydrationStrategy: NadelHydrationStrategy.ManyToOne,
    ): List<Map<String, NormalizedInputValue>> {
        val argumentToSplit = hydrationStrategy.argumentToSplit
        val sharedArgs = makeInputMap(excluding = argumentToSplit)

        return getResultNodes(argumentToSplit)
            .asSequence()
            .flatMap { node ->
                // This code belongs together for cohesiveness, do NOT merge it back into the outer sequence
                sequenceOf(node.value)
                    .flatten(recursively = true)
                    .map {
                        makeInputValue(hydrationArgument = argumentToSplit, value = it)
                    }
            }
            .map {
                // Make the pair to go along with every input map
                argumentToSplit.name to it
            }
            .map {
                sharedArgs + it
            }
            .filter {
                isInputMapValid(it)
            }
            .toList()
    }

    private fun makeInputMap(
        excluding: NadelHydrationArgument? = null,
    ): Map<String, NormalizedInputValue> {
        return instruction.backingFieldArguments
            .asSequence()
            .filter {
                it != excluding
            }
            .mapNotNull { inputDef ->
                makeInputValuePair(inputDef)
            }
            .toMapStrictly()
    }

    /**
     * Valid in the sense that we should actually query for data.
     *
     * If all field values are null, then it doesn't makes sense to actually send the query.
     */
    private fun isInputMapValid(inputMap: Map<String, NormalizedInputValue>): Boolean {
        val fieldInputsNames = instruction.backingFieldArguments
            .asSequence()
            .filterIsInstance<NadelHydrationArgument.SourceField>()
            .map { it.name }
            .toList()

        // My brain hurts, checking if it's invalid makes a lot more sense to me. So we invert it at the end
        return !(fieldInputsNames.isNotEmpty() && fieldInputsNames.all { inputName ->
            inputMap[inputName]?.value is NullValue?
        })
    }

    private fun makeInputValuePair(
        inputDef: NadelHydrationArgument,
    ): Pair<String, NormalizedInputValue>? {
        val inputValue = makeInputValue(inputDef) ?: return null
        return inputDef.name to inputValue
    }

    private fun makeInputValue(
        hydrationArgument: NadelHydrationArgument,
    ): NormalizedInputValue? {
        return when (hydrationArgument) {
            is NadelHydrationArgument.VirtualFieldArgument -> getArgumentValue(hydrationArgument)
            is NadelHydrationArgument.SourceField -> makeInputValue(
                hydrationArgument,
                value = getResultValue(hydrationArgument),
            )
            is NadelHydrationArgument.StaticValue -> hydrationArgument.normalizedInputValue
            is NadelHydrationArgument.RemainingVirtualFieldArguments -> NormalizedInputValue(
                /* typeName = */ GraphQLTypeUtil.simplePrint(hydrationArgument.backingArgumentDef.type),
                /* value = */
                hydrationArgument.remainingArgumentNames
                    .associateWith {
                        virtualField.normalizedArguments[it]?.value
                    },
            )
        }
    }

    private fun makeInputValue(
        hydrationArgument: NadelHydrationArgument,
        value: Any?,
    ): NormalizedInputValue {
        return makeNormalizedInputValue(
            type = hydrationArgument.backingArgumentDef.type,
            value = javaValueToAstValue(value),
        )
    }

    private fun getArgumentValue(
        valueSource: NadelHydrationArgument.VirtualFieldArgument,
    ): NormalizedInputValue? {
        return virtualField.getNormalizedArgument(valueSource.virtualFieldArgumentName) ?: valueSource.defaultValue
    }

    private fun getResultValue(
        valueSource: NadelHydrationArgument.SourceField,
    ): Any? {
        return getResultNodes(valueSource)
            .emptyOrSingle()
            ?.value
    }

    private fun getResultNodes(
        valueSource: NadelHydrationArgument.SourceField,
    ): List<JsonNode> {
        return JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.getQueryPath(valueSource.pathToSourceField),
        )
    }
}
