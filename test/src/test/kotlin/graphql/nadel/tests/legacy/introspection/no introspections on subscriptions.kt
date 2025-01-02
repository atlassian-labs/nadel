package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `no introspections on subscriptions` : NadelLegacyIntegrationTest(query = """
|subscription {
|  __typename
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  comment: Comment
    |}
    |type Subscription {
    |  onComment: Comment @namespaced
    |}
    |type Comment {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  comment: Comment
    |}
    |type Subscription {
    |  onComment: Comment
    |}
    |type Comment {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class MyService_Comment(
    public val id: String? = null,
  )
}
