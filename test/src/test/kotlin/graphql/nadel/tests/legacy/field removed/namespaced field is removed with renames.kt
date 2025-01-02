package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `namespaced field is removed with renames` : NadelLegacyIntegrationTest(query = """
|query {
|  commentApi {
|    commentById(id: "C1") {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="CommentService",
    overallSchema="""
    |directive @toBeDeleted on FIELD_DEFINITION
    |directive @namespaced on FIELD_DEFINITION
    |type Query {
    |  commentApi: CommentApi @namespaced
    |}
    |type CommentApi @renamed(from: "CommentApiUnderlying") {
    |  commentById(id: ID): Comment @toBeDeleted @renamed(from: "commentByIdUnderlying")
    |}
    |type Comment {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  commentApi: CommentApiUnderlying
    |}
    |type CommentApiUnderlying {
    |  commentByIdUnderlying(id: ID): Comment
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

  private data class CommentService_CommentApiUnderlying(
    public val commentByIdUnderlying: CommentService_Comment? = null,
  )
}
