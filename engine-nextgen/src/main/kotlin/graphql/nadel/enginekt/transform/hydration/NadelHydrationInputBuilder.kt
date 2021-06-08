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
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInput
import graphql.nadel.enginekt.transform.artificial.AliasHelper
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getSourceFieldDefinition
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLTypeUtil
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource as ValueSource

internal typealias AnyAstValue = Value<*>

internal object NadelHydrationInputBuilder {
    fun getInputValues(
        instruction: NadelHydrationFieldInstruction,
        aliasHelper: AliasHelper,
        hydrationField: NormalizedField,
        parentNode: JsonNode,
    ): Map<String, NormalizedInputValue> {
        val sourceField = getSourceFieldDefinition(instruction)

        return mapFrom(
            instruction.actorInputValues
                .map {
                    val argumentDef = sourceField.getArgument(it.name)
                    it.name to makeInputValue(it, argumentDef, parentNode, hydrationField, aliasHelper)
                }
        )
    }

    private fun makeInputValue(
        actorInput: NadelHydrationActorInput,
        argumentDef: GraphQLArgument,
        parentNode: JsonNode,
        hydrationField: NormalizedField,
        aliasHelper: AliasHelper,
    ): NormalizedInputValue {
        return when (val valueSource = actorInput.valueSource) {
            is ValueSource.ArgumentValue -> hydrationField.getNormalizedArgument(valueSource.argumentName)
            is ValueSource.FieldResultValue -> makeInputValue(argumentDef) {
                getFieldValue(valueSource, parentNode, aliasHelper)
            }
        }
    }

    private fun getFieldValue(
        valueSource: ValueSource.FieldResultValue,
        parentNode: JsonNode,
        aliasHelper: AliasHelper,
    ): AnyAstValue {
        val value = JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryPath = aliasHelper.mapQueryPathRespectingResultKey(valueSource.queryPathToField),
        ).emptyOrSingle()?.value

        return valueToAstValue(value)
    }

    internal fun valueToAstValue(value: Any?): AnyAstValue {
        return when (value) {
            is AnyList -> ArrayValue(
                value.map(this::valueToAstValue),
            )
            is AnyMap -> ObjectValue
                .newObjectValue()
                .objectFields(
                    value.let {
                        @Suppress("UNCHECKED_CAST")
                        it as JsonMap
                    }.map {
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

    private inline fun makeInputValue(
        argumentDef: GraphQLArgument,
        valueFactory: () -> AnyAstValue,
    ): NormalizedInputValue {
        return NormalizedInputValue(
            GraphQLTypeUtil.simplePrint(argumentDef.type), // Type name
            valueFactory(),
        )
    }
}
