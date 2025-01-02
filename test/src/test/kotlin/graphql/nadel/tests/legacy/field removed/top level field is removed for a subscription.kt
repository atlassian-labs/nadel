package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `top level field is removed for a subscription` : NadelLegacyIntegrationTest(query =
    """
|subscription {
|  onCommentUpdated(id: "C1") {
|    id
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="CommentService",
    overallSchema="""
    |directive @toBeDeleted on FIELD_DEFINITION
    |type Query {
    |  commentById(id: ID): Comment
    |}
    |type Subscription {
    |  onCommentUpdated(id: ID): Comment @toBeDeleted
    |}
    |type Comment {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  commentById(id: ID): Comment
    |}
    |type Subscription {
    |  onCommentUpdated(id: ID): Comment
    |}
    |type Comment {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class CommentService_Comment(
    public val id: String? = null,
  )
}
