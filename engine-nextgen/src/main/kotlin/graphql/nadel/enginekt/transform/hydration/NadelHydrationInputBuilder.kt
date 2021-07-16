package graphql.nadel.enginekt.transform.hydration

import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef.ValueSource
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.asJsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLTypeUtil

internal typealias AnyAstValue = Value<*>

internal object NadelHydrationInputBuilder {
    fun getInputValues(
        instruction: NadelHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        hydratedField: ExecutableNormalizedField,
        parentNode: JsonNode,
    ): List<Map<String, NormalizedInputValue>> {
        val inputDefsForAllCalls = instruction.actorInputValueDefs.asSequence()
            .let { defs ->
                when (val hydrationStrategy = instruction.hydrationStrategy) {
                    is NadelHydrationStrategy.OneToOne -> defs
                    is NadelHydrationStrategy.ManyToOne -> defs.filter { def -> def != hydrationStrategy.inputDefToSplit }
                }
            }

        val argsForAllCalls = mapFrom(
            inputDefsForAllCalls
                .mapNotNull { actorInputDef ->
                    val argumentDef = instruction.actorFieldDef.getArgument(actorInputDef.name)
                    val inputValue = makeInputValue(
                        actorInputDef = actorInputDef,
                        argumentDef = argumentDef,
                        parentNode = parentNode,
                        hydrationField = hydratedField,
                        aliasHelper = aliasHelper,
                    ) ?: return@mapNotNull null
                    actorInputDef.name to inputValue
                }
                .toList()
        )

        return when (val hydrationStrategy = instruction.hydrationStrategy) {
            is NadelHydrationStrategy.OneToOne -> listOf(argsForAllCalls)
            is NadelHydrationStrategy.ManyToOne -> {
                val inputDefToSplit = hydrationStrategy.inputDefToSplit
                val valuesToSplit = when (val valueSource = inputDefToSplit.valueSource) {
                    is ValueSource.FieldResultValue -> getResultValues(valueSource, parentNode, aliasHelper)
                    else -> error("Can only split field result value into multiple hydration calls")
                }
                valuesToSplit
                    .asSequence()
                    .flatten(recursively = true) // Honestly: I think we need to revisit this, we kind of make big assumptions on the schema
                    .map { value ->
                        val inputValuePerCall = inputDefToSplit.name to makeNormalizedInputValue(
                            inputDefToSplit.actorArgumentDef,
                            value = valueToAstValue(value),
                        )
                        argsForAllCalls + inputValuePerCall
                    }
                    .toList()
            }
        }
    }

    private fun makeInputValue(
        actorInputDef: NadelHydrationActorInputDef,
        argumentDef: GraphQLArgument,
        parentNode: JsonNode,
        hydrationField: ExecutableNormalizedField,
        aliasHelper: NadelAliasHelper,
    ): NormalizedInputValue? {
        return when (val valueSource = actorInputDef.valueSource) {
            is ValueSource.ArgumentValue -> hydrationField.getNormalizedArgument(valueSource.argumentName)
            is ValueSource.FieldResultValue -> makeNormalizedInputValue(
                argumentDef,
                value = valueToAstValue(
                    getResultValue(valueSource, parentNode, aliasHelper),
                ),
            )
        }
    }

    private fun getResultValue(
        valueSource: ValueSource.FieldResultValue,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
    ): Any? {
        return JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.getQueryPath(valueSource.queryPathToField),
        ).emptyOrSingle()?.value
    }

    private fun getResultValues(
        valueSource: ValueSource.FieldResultValue,
        parentNode: JsonNode,
        aliasHelper: NadelAliasHelper,
    ): List<Any?> {
        return JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.getQueryPath(valueSource.queryPathToField),
        ).map {
            it.value
        }
    }

    private fun makeNormalizedInputValue(
        argumentDef: GraphQLArgument,
        value: AnyAstValue,
    ): NormalizedInputValue {
        return NormalizedInputValue(
            GraphQLTypeUtil.simplePrint(argumentDef.type), // type name
            value, // value
        )
    }

    internal fun valueToAstValue(value: Any?): AnyAstValue {
        return when (value) {
            is AnyList -> ArrayValue(
                value.map(this::valueToAstValue),
            )
            is AnyMap -> ObjectValue
                .newObjectValue()
                .objectFields(
                    value.asJsonMap().map {
                        ObjectField(it.key, valueToAstValue(it.value))
                    },
                )
                .build()
            null -> NullValue
                .newNullValue()
                .build()
            is Double -> FloatValue.newFloatValue()
                .value(value.toBigDecimal())
                .build()
            is Float -> FloatValue.newFloatValue()
                .value(value.toBigDecimal())
                .build()
            is Number -> IntValue.newIntValue()
                .value(value.toLong().toBigInteger())
                .build()
            is String -> StringValue.newStringValue()
                .value(value)
                .build()
            is Boolean -> BooleanValue.newBooleanValue()
                .value(value)
                .build()
            else -> error("Unknown value type '${value.javaClass.name}'")
        }
    }
}
