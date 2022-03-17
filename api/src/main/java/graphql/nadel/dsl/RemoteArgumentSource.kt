package graphql.nadel.dsl

// todo this should be a union or sealed class thing
data class RemoteArgumentSource(
    val argumentName: String?, // for OBJECT_FIELD
    val pathToField: List<String>?,
    val sourceType: SourceType?,
) {
    enum class SourceType {
        ObjectField,
        FieldArgument,
    }
}
