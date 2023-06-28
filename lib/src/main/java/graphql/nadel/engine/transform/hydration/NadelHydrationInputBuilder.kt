package graphql.nadel.engine.transform.hydration

import graphql.language.NullValue
import graphql.nadel.NadelEngineContext
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgumentDef
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgumentDef.ValueSource
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.toMapStrictly
import graphql.normalized.NormalizedInputValue

context(NadelEngineContext, NadelExecutionContext, NadelHydrationTransformContext)
internal class NadelHydrationInputBuilder private constructor(
    private val instruction: NadelHydrationFieldInstruction,
    private val parentNode: JsonNode,
) {
    companion object {
        context(NadelEngineContext, NadelExecutionContext, NadelHydrationTransformContext)
        fun getInputValues(
            instruction: NadelHydrationFieldInstruction,
            parentNode: JsonNode,
        ): List<Map<String, NormalizedInputValue>> {
            return NadelHydrationInputBuilder(instruction, parentNode)
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
        val inputDefToSplit = hydrationStrategy.inputDefToSplit
        val valueSourceToSplit = inputDefToSplit.valueSource as ValueSource.FromResultValue
        val sharedArgs = makeInputMap(inputDefToSplit)

        return getResultNodes(valueSourceToSplit)
            .asSequence()
            .flatMap { node ->
                // This code belongs together for cohesiveness, do NOT merge it back into the outer sequence
                sequenceOf(node.value)
                    .flatten(recursively = true)
                    .map {
                        makeInputValue(inputDef = inputDefToSplit, value = it)
                    }
            }
            .map {
                // Make the pair to go along with every input map
                inputDefToSplit.name to it
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
        excluding: NadelHydrationArgumentDef? = null,
    ): Map<String, NormalizedInputValue> {
        return instruction.effectFieldArgDefs
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
        val fieldInputsNames = instruction.effectFieldArgDefs
            .asSequence()
            .filter {
                it.valueSource is ValueSource.FromResultValue
            }
            .map { it.name }
            .toList()

        // My brain hurts, checking if it's invalid makes a lot more sense to me. So we invert it at the end
        return !(fieldInputsNames.isNotEmpty() && fieldInputsNames.all { inputName ->
            inputMap[inputName]?.value is NullValue?
        })
    }

    private fun makeInputValuePair(
        inputDef: NadelHydrationArgumentDef,
    ): Pair<String, NormalizedInputValue>? {
        val inputValue = makeInputValue(inputDef) ?: return null
        return inputDef.name to inputValue
    }

    private fun makeInputValue(
        inputDef: NadelHydrationArgumentDef,
    ): NormalizedInputValue? {
        return when (val valueSource = inputDef.valueSource) {
            is ValueSource.FromArgumentValue -> getArgumentValue(valueSource)
            is ValueSource.FromResultValue -> makeInputValue(
                inputDef,
                value = getResultValue(valueSource),
            )
        }
    }

    private fun makeInputValue(
        inputDef: NadelHydrationArgumentDef,
        value: Any?,
    ): NormalizedInputValue {
        return makeNormalizedInputValue(
            type = inputDef.effectArgumentDef.type,
            value = javaValueToAstValue(value),
        )
    }

    private fun getArgumentValue(
        valueSource: ValueSource.FromArgumentValue,
    ): NormalizedInputValue? {
        return hydrationCauseField.getNormalizedArgument(valueSource.argumentName)
            ?: valueSource.defaultValue
    }

    private fun getResultValue(
        valueSource: ValueSource.FromResultValue,
    ): Any? {
        return getResultNodes(valueSource)
            .emptyOrSingle()
            ?.value
    }

    private fun getResultNodes(
        valueSource: ValueSource.FromResultValue,
    ): List<JsonNode> {
        return JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.getQueryPath(valueSource.queryPathToField),
        )
    }
}
