package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `hidden namespaced hydration top level field is removed` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issueById(id: "C1") {
|    id
|    comment {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |directive @namespaced on FIELD_DEFINITION
    |type Query {
    |  issueById(id: ID): Issue @namespaced
    |}
    |type Issue {
    |  id: ID
    |  comment: Comment @hydrated(
    |    service: "CommentService"
    |    field: "commentApi.commentById"
    |    arguments: [
    |      {name: "id", value: "${'$'}source.commentId"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issueById(id: ID): Issue
    |}
    |type Issue {
    |  id: ID
    |  commentId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issueById") { env ->
          if (env.getArgument<Any?>("id") == "C1") {
            IssueService_Issue(commentId = "C1", id = "C1")}
          else {
            null}
        }
      }
    }
    )
, Service(name="CommentService", overallSchema="""
    |directive @toBeDeleted on FIELD_DEFINITION
    |type Query {
    |  commentApi: CommentApi @namespaced @hidden
    |  echo: String
    |}
    |type CommentApi {
    |  commentById(id: ID): Comment @toBeDeleted @hidden
    |  echo: String
    |}
    |type Comment {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  commentApi: CommentApi
    |  echo: String
    |}
    |type CommentApi {
    |  commentById(id: ID): Comment
    |  echo: String
    |}
    |type Comment {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class IssueService_Issue(
    public val id: String? = null,
    public val commentId: String? = null,
  )

  private data class CommentService_Comment(
    public val id: String? = null,
  )

  private data class CommentService_CommentApi(
    public val commentById: CommentService_Comment? = null,
    public val echo: String? = null,
  )
}
