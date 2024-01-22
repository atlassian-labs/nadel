package graphql.nadel.dsl

import graphql.nadel.util.AnyAstValue

sealed class RemoteArgumentSource {
    data class ObjectField(
        val pathToField: List<String>,
    ) : RemoteArgumentSource()

    data class FieldArgument(
        val argumentName: String,
    ) : RemoteArgumentSource()

    data class StaticArgument(
        val staticValue: AnyAstValue,
    ) : RemoteArgumentSource()
}
