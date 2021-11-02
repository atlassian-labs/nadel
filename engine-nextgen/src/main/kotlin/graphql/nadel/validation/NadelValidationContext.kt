package graphql.nadel.validation

internal data class NadelValidationContext(
    val visitedTypes: MutableSet<NadelServiceSchemaElementRef> = hashSetOf(),
)
