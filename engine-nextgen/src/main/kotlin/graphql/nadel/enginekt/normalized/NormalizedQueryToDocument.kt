package graphql.nadel.enginekt.normalized

import graphql.language.Argument
import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.Document
import graphql.language.Field
import graphql.language.FloatValue
import graphql.language.InlineFragment
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.OperationDefinition
import graphql.language.Selection
import graphql.language.SelectionSet
import graphql.language.StringValue
import graphql.language.TypeName
import graphql.language.Value
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.AnyMapEntry
import graphql.nadel.enginekt.util.JsonMapEntry
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedQueryTree

class NormalizedQueryToDocument {
    fun toDocument(query: NormalizedQueryTree): Document {
        return Document.newDocument()
            .definition(
                OperationDefinition.newOperationDefinition()
                    .selectionSet(
                        SelectionSet.newSelectionSet()
                            .selections(query.topLevelFields.map(this::mapField))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    fun toDocument(field: NormalizedField): Document {
        return Document.newDocument()
            .definition(
                OperationDefinition.newOperationDefinition()
                    .selectionSet(
                        SelectionSet.newSelectionSet()
                            .selection(mapField(field))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun mapField(field: NormalizedField): Selection<*> {
        val fieldSelection = Field.newField()
            .alias(field.alias)
            .name(field.name)
            .arguments(field.arguments.map(::mapArgument))
            .build()

        if (field.isConditional) {
            return InlineFragment.newInlineFragment()
                .selectionSet(SelectionSet.newSelectionSet().selection(fieldSelection).build())
                .typeCondition(TypeName(field.objectType.name))
                .build()
        }

        return fieldSelection
    }

    private fun mapArgument(argument: JsonMapEntry): Argument {
        return Argument(argument.key, mapValue(argument.value))
    }

    private fun mapValue(value: Any?): Value<*>? {
        return when (value) {
            is AnyMap -> ObjectValue(value.entries.map(this::mapObjectField))
            is AnyList -> ArrayValue(value.map(this::mapValue))
            is Boolean -> BooleanValue(value)
            is Float -> FloatValue(value.toBigDecimal())
            is Double -> FloatValue(value.toBigDecimal())
            is Number -> IntValue(value.toLong().toBigInteger())
            is String -> StringValue(value)
            null -> NullValue.newNullValue().build()
            else -> throw UnsupportedOperationException("Unknown type $value")
        }
    }

    private fun mapObjectField(entry: AnyMapEntry): ObjectField {
        return ObjectField(entry.key as String, mapValue(entry.value))
    }
}
