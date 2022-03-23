package graphql.nadel.validation

data class NadelValidationTestFixture(
    val overallSchema: Map<String, String>,
    val underlyingSchema: Map<String, String>,
)
