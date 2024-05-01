package graphql.nadel.tests.next

import org.intellij.lang.annotations.Language

abstract class TestData {
    abstract val calls: List<ExpectedServiceCall>
    abstract val response: ExpectedNadelResponse
}

data class ExpectedServiceCall(
    val service: String,
    @Language("GraphQL")
    val query: String,
    @Language("JSON")
    val variables: String,
    @Language("JSON")
    val response: String,
    val delayedResponses: List<String>,
)

data class ExpectedNadelResponse(
    @Language("JSON")
    val response: String?,
    val delayedResponses: List<String>,
)
