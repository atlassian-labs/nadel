package graphql.nadel.enginekt.transform.hydration

import graphql.language.BooleanValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtils.getSourceField
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

internal object NadelHydrationArgumentsBuilder {
    fun createSourceFieldArgs(
        service: Service,
        instruction: NadelHydrationFieldInstruction,
        parentNode: JsonNode,
        hydrationField: NormalizedField,
    ): Map<String, NormalizedInputValue> {
        val sourceField = getSourceField(service, instruction.pathToSourceField)

        return mapFrom(
            instruction.arguments
                .map {
                    val argumentDef = sourceField.getArgument(it.name)
                    it.name to makeInputValue(it, argumentDef, parentNode, hydrationField)
                }
        )
    }

    private fun makeInputValue(
        argument: NadelHydrationArgument,
        argumentDef: GraphQLArgument,
        parentNode: JsonNode,
        hydrationField: NormalizedField,
    ): NormalizedInputValue {
        return when (val valueSource = argument.valueSource) {
            is ValueSource.ArgumentValue -> hydrationField.getNormalizedArgument(valueSource.argumentName)
            is ValueSource.FieldValue -> makeInputValue(argumentDef) {
                getFieldValue(valueSource, parentNode)
            }
        }
    }

    private fun getFieldValue(
        valueSource: ValueSource.FieldValue,
        parentNode: JsonNode,
    ): AnyNormalizedInputValueValue {
        val value = JsonNodeExtractor.getNodesAt(
            rootNode = parentNode,
            queryResultKeyPath = valueSource.pathToField,
        ).emptyOrSingle()?.value

        return when (value) {
            is AnyList -> NormalizedInputValueValue.ListValue(value)
            // TODO: I think this needs to be reconstructed with leaf values as graphql.language.Value
            is AnyMap -> @Suppress("UNCHECKED_CAST") NormalizedInputValueValue.ObjectValue(value as JsonMap)
            null -> NormalizedInputValueValue.AstValue(NullValue.newNullValue().build())
            is Double -> NormalizedInputValueValue.AstValue(FloatValue.newFloatValue().value(
                value.toBigDecimal(),
            ).build())
            is Float -> NormalizedInputValueValue.AstValue(FloatValue.newFloatValue().value(
                value.toBigDecimal(),
            ).build())
            is Number -> NormalizedInputValueValue.AstValue(IntValue.newIntValue().value(
                value.toLong().toBigInteger(),
            ).build())
            is String -> NormalizedInputValueValue.AstValue(StringValue.newStringValue().value(value).build())
            is Boolean -> NormalizedInputValueValue.AstValue(BooleanValue.newBooleanValue().value(value).build())
            else -> error("Unknown value type '${value.javaClass.name}'")
        }
    }

    private inline fun makeInputValue(
        argumentDef: GraphQLArgument,
        valueFactory: () -> AnyNormalizedInputValueValue,
    ): NormalizedInputValue {
        return NormalizedInputValue(
            GraphQLTypeUtil.simplePrint(argumentDef.type), // Type name
            valueFactory().value,
        )
    }
}

private typealias AnyNormalizedInputValueValue = NormalizedInputValueValue<*>

internal sealed class NormalizedInputValueValue<T> {
    abstract val value: T

    data class ListValue<T>(
        override val value: List<T>,
    ) : NormalizedInputValueValue<List<T>>()

    data class ObjectValue(
        override val value: JsonMap,
    ) : NormalizedInputValueValue<JsonMap>()

    @Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA") // idk
    data class AstValue<T : Value<*>>(
        override val value: T,
    ) : NormalizedInputValueValue<T>()
}
