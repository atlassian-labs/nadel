package graphql.nadel.validation

data class NadelValidationContext(
    val visitedTypes: MutableSet<NadelServiceSchemaElementRef> = hashSetOf(),
)
