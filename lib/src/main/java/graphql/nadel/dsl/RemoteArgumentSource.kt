package graphql.nadel.dsl

import graphql.language.Value

// todo this should be a union or sealed class thing
data class RemoteArgumentSource(
    val argumentName: String?, // for OBJECT_FIELD
    val pathToField: List<String>?,
    val staticValue: Value<*>?,
    val sourceType: SourceType,
) {
    enum class SourceType {
        ObjectField,
        FieldArgument,
        StaticArgument
    }
}
