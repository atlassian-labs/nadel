package graphql.nadel.util

import graphql.introspection.Introspection
import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.Description
import graphql.language.DirectiveDefinition
import graphql.language.DirectiveLocation
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.IntValue
import graphql.language.ListType
import graphql.language.ListType.newListType
import graphql.language.NamedNode
import graphql.language.NonNullType
import graphql.language.NonNullType.newNonNullType
import graphql.language.Type
import graphql.language.TypeName
import graphql.language.TypeName.newTypeName
import graphql.language.Value
import graphql.schema.GraphQLNamedInputType
import graphql.schema.GraphQLNamedSchemaElement

internal val DirectiveOnQuery = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.QUERY.name)
    .build()
internal val DirectiveOnMutation = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.MUTATION.name)
    .build()
internal val DirectiveOnSubscription = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.SUBSCRIPTION.name)
    .build()
internal val DirectiveOnField = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.FIELD.name)
    .build()
internal val DirectiveOnFragmentDefinition = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.FRAGMENT_DEFINITION.name)
    .build()
internal val DirectiveOnFragmentSpread = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.FRAGMENT_SPREAD.name)
    .build()
internal val DirectiveOnInlineFragment = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.INLINE_FRAGMENT.name)
    .build()
internal val DirectiveOnVariableDefinition = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.VARIABLE_DEFINITION.name)
    .build()
internal val DirectiveOnSchema = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.SCHEMA.name)
    .build()
internal val DirectiveOnScalar = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.SCALAR.name)
    .build()
internal val DirectiveOnObject = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.OBJECT.name)
    .build()
internal val DirectiveOnFieldDefinition = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.FIELD_DEFINITION.name)
    .build()
internal val DirectiveOnArgumentDefinition = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.ARGUMENT_DEFINITION.name)
    .build()
internal val DirectiveOnInterface = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.INTERFACE.name)
    .build()
internal val DirectiveOnUnion = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.UNION.name)
    .build()
internal val DirectiveOnEnum = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.ENUM.name)
    .build()
internal val DirectiveOnEnumValue = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.ENUM_VALUE.name)
    .build()
internal val DirectiveOnInputObject = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.INPUT_OBJECT.name)
    .build()
internal val DirectiveOnInputFieldDefinition = DirectiveLocation.newDirectiveLocation()
    .name(Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION.name)
    .build()

internal fun DirectiveDefinition.Builder.onQuery(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnQuery)
}

internal fun DirectiveDefinition.Builder.onMutation(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnMutation)
}

internal fun DirectiveDefinition.Builder.onSubscription(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnSubscription)
}

internal fun DirectiveDefinition.Builder.onField(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnField)
}

internal fun DirectiveDefinition.Builder.onFragmentDefinition(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnFragmentDefinition)
}

internal fun DirectiveDefinition.Builder.onFragmentSpread(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnFragmentSpread)
}

internal fun DirectiveDefinition.Builder.onInlineFragment(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnInlineFragment)
}

internal fun DirectiveDefinition.Builder.onVariableDefinition(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnVariableDefinition)
}

internal fun DirectiveDefinition.Builder.onSchema(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnSchema)
}

internal fun DirectiveDefinition.Builder.onScalar(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnScalar)
}

internal fun DirectiveDefinition.Builder.onObject(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnObject)
}

internal fun DirectiveDefinition.Builder.onFieldDefinition(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnFieldDefinition)
}

internal fun DirectiveDefinition.Builder.onArgumentDefinition(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnArgumentDefinition)
}

internal fun DirectiveDefinition.Builder.onInterface(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnInterface)
}

internal fun DirectiveDefinition.Builder.onUnion(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnUnion)
}

internal fun DirectiveDefinition.Builder.onEnum(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnEnum)
}

internal fun DirectiveDefinition.Builder.onEnumValue(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnEnumValue)
}

internal fun DirectiveDefinition.Builder.onInputObject(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnInputObject)
}

internal fun DirectiveDefinition.Builder.onInputFieldDefinition(): DirectiveDefinition.Builder {
    return directiveLocation(DirectiveOnInputFieldDefinition)
}

internal fun DirectiveDefinition.Builder.inputValueDefinition(
    name: String,
    type: Type<*>,
    description: String? = null,
    defaultValue: Value<*>? = null,
): DirectiveDefinition.Builder {
    return inputValueDefinition(
        InputValueDefinition.newInputValueDefinition()
            .name(name)
            .type(type)
            .also { builder ->
                if (description != null) builder.description(Description(description, null, false))
                if (defaultValue != null) builder.defaultValue(defaultValue)
            }
            .build()
    )
}

internal fun DirectiveDefinition.Builder.inputValueDefinition(
    name: String,
    type: GraphQLNamedInputType,
    description: String? = null,
    defaultValue: Value<*>? = null,
): DirectiveDefinition.Builder {
    return inputValueDefinition(
        name = name,
        type = newTypeName(type.name).build(),
        description = description,
        defaultValue = defaultValue,
    )
}

internal fun InputObjectTypeDefinition.Builder.inputValueDefinition(
    name: String,
    type: Type<*>,
    description: String? = null,
    defaultValue: Value<*>? = null,
): InputObjectTypeDefinition.Builder {
    return inputValueDefinition(
        InputValueDefinition.newInputValueDefinition()
            .name(name)
            .type(type)
            .also { builder ->
                if (description != null) builder.description(Description(description, null, false))
                if (defaultValue != null) builder.defaultValue(defaultValue)
            }
            .build()
    )
}

internal fun InputObjectTypeDefinition.Builder.inputValueDefinition(
    name: String,
    type: GraphQLNamedInputType,
    description: String? = null,
    defaultValue: Value<*>? = null,
): InputObjectTypeDefinition.Builder {
    return inputValueDefinition(
        name = name,
        type = newTypeName(type.name).build(),
        description = description,
        defaultValue = defaultValue,
    )
}

internal fun InputObjectTypeDefinition.Builder.description(description: String): InputObjectTypeDefinition.Builder {
    return description(Description(description, null, false))
}

internal fun DirectiveDefinition.Builder.description(description: String): DirectiveDefinition.Builder {
    return description(Description(description, null, false))
}

internal fun IntValue(value: Int): IntValue {
    return IntValue.newIntValue().value(value).build()
}

internal fun BooleanValue(value: Boolean): BooleanValue {
    return BooleanValue.newBooleanValue(value).build()
}

internal fun emptyArrayValue(): ArrayValue {
    return ArrayValue.newArrayValue().build()
}

internal fun nonNull(type: GraphQLNamedSchemaElement): NonNullType {
    return newNonNullType(
        newTypeName()
            .name(type.name)
            .build(),
    ).build()
}

internal fun nonNull(type: NamedNode<*>): NonNullType {
    return newNonNullType(
        newTypeName()
            .name(type.name)
            .build(),
    ).build()
}

internal fun list(type: NonNullType): ListType {
    return newListType(type).build()
}

internal fun nonNull(type: Type<*>): NonNullType {
    return newNonNullType(type).build()
}

