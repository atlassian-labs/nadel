package graphql.nadel.enginekt.blueprint.hydration

data class HydrationArgument(
    val name: String,
    val valueSource: HydrationArgumentValueSource,
)

sealed class HydrationArgumentValueSource {

    data class FieldValue(val pathToField: List<String>) : HydrationArgumentValueSource()

    data class ArgumentValue(val argumentName: String) : HydrationArgumentValueSource()
}
