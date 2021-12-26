package graphql.nadel.enginekt.transform.hydration

import graphql.language.NullValue
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef.ValueSource
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.CountedBox
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.javaValueToAstValue
import graphql.nadel.enginekt.util.makeNormalizedInputValue
import graphql.nadel.enginekt.util.toMapStrictly
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

internal class NadelHydrationInputBuilder private constructor(
    private val instruction: NadelHydrationFieldInstruction,
    private val aliasHelper: NadelAliasHelper,
    private val fieldToHydrate: ExecutableNormalizedField,
    private val parentNode: JsonNode,
) {
    companion object {
        fun getInputValues(
            instruction: NadelHydrationFieldInstruction,
            aliasHelper: NadelAliasHelper,
            fieldToHydrate: ExecutableNormalizedField,
            parentNode: JsonNode,
        ): List<Map<String, CountedBox<NormalizedInputValue>>> {
            return NadelHydrationInputBuilder(instruction, aliasHelper, fieldToHydrate, parentNode)
                .build()
        }
    }

    private fun build(): List<Map<String, CountedBox<NormalizedInputValue>>> {
        return when (val hydrationStrategy = instruction.hydrationStrategy) {
            is NadelHydrationStrategy.OneToOne -> makeOneToOneArgs()
            is NadelHydrationStrategy.ManyToOne -> makeManyToOneArgs(hydrationStrategy)
        }
    }

    private fun makeOneToOneArgs(): List<Map<String, CountedBox<NormalizedInputValue>>> {
        return listOfNotNull(
            makeInputMap().takeIf(::isInputMapValid),
        )
    }

    private fun makeManyToOneArgs(
        hydrationStrategy: NadelHydrationStrategy.ManyToOne,
    ): List<Map<String, CountedBox<NormalizedInputValue>>> {
        val inputDefToSplit = hydrationStrategy.inputDefToSplit
        val valueSourceToSplit = inputDefToSplit.valueSource as ValueSource.FieldResultValue
        val sharedArgs = makeInputMap(inputDefToSplit)

        val resultNodes = getResultNodes(valueSourceToSplit)
        return resultNodes
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
                CountedBox(it,resultNodes.size)
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
        excluding: NadelHydrationActorInputDef? = null,
    ): Map<String, CountedBox<NormalizedInputValue>> {
        return instruction.actorInputValueDefs
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
    private fun isInputMapValid(inputMap: Map<String, CountedBox<NormalizedInputValue>>): Boolean {
        val fieldInputsNames = instruction.actorInputValueDefs
            .asSequence()
            .filter {
                it.valueSource is ValueSource.FieldResultValue
            }
            .map { it.name }
            .toList()

        // My brain hurts, checking if it's invalid makes a lot more sense to me. So we invert it at the end
        return !(fieldInputsNames.isNotEmpty() && fieldInputsNames.all { inputName ->
            val hydrationObject = inputMap[inputName]?.boxedObject
            hydrationObject?.value is NullValue?
        })
    }

    private fun makeInputValuePair(
        inputDef: NadelHydrationActorInputDef,
    ): Pair<String, CountedBox<NormalizedInputValue>>? {
        val inputValue = makeInputValue(inputDef) ?: return null
        return inputDef.name to CountedBox(inputValue,1)
    }

    private fun makeInputValue(
        inputDef: NadelHydrationActorInputDef,
    ): NormalizedInputValue? {
        return when (val valueSource = inputDef.valueSource) {
            is ValueSource.ArgumentValue -> fieldToHydrate.getNormalizedArgument(valueSource.argumentName)
            is ValueSource.FieldResultValue -> makeInputValue(
                inputDef,
                value = getSingleResultValue(valueSource),
            )
        }
    }

    private fun makeInputValue(
        inputDef: NadelHydrationActorInputDef,
        value: Any?,
    ): NormalizedInputValue {
        return makeNormalizedInputValue(
            type = inputDef.actorArgumentDef.type,
            value = javaValueToAstValue(value),
        )
    }

    private fun getSingleResultValue(
        valueSource: ValueSource.FieldResultValue,
    ): Any? {
        return getResultNodes(valueSource)
            .emptyOrSingle()
            ?.value
    }

    private fun getResultNodes(
        valueSource: ValueSource.FieldResultValue,
    ): List<JsonNode> {
        return JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.getQueryPath(valueSource.queryPathToField),
        )
    }
}
