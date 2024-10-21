package graphql.nadel.dsl

import graphql.nadel.util.AnyAstValue

sealed class RemoteArgumentSource {
    @Deprecated(
        replaceWith = ReplaceWith(
            "NadelHydrationArgumentDefinition.ValueSource.ObjectField",
            imports = ["graphql.nadel.engine.blueprint.directives.NadelHydrationArgumentDefinition"]
        ),
        message = "Do not use",
    )
    data class ObjectField(
        val pathToField: List<String>,
    ) : RemoteArgumentSource()

    @Deprecated(
        replaceWith = ReplaceWith(
            "NadelHydrationArgumentDefinition.ValueSource.FieldArgument",
            imports = ["graphql.nadel.engine.blueprint.directives.NadelHydrationArgumentDefinition"]
        ),
        message = "Do not use",
    )
    data class FieldArgument(
        val argumentName: String,
    ) : RemoteArgumentSource()

    @Deprecated(
        replaceWith = ReplaceWith(
            "NadelHydrationArgumentDefinition.ValueSource.StaticArgument",
            imports = ["graphql.nadel.engine.blueprint.directives.NadelHydrationArgumentDefinition"]
        ),
        message = "Do not use",
    )
    data class StaticArgument(
        val staticValue: AnyAstValue,
    ) : RemoteArgumentSource()
}
