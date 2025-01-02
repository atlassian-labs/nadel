package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.Int
import kotlin.String

public class `hydration within hydration with variables` : NadelLegacyIntegrationTest(query = """
|query(${'$'}first: Int) {
|  board {
|    issue {
|      comments(first: ${'$'}first) {
|        totalCount
|      }
|    }
|  }
|}
|""".trimMargin(), variables = mapOf("first" to 10), services = listOf(Service(name="boards",
    overallSchema="""
    |type Query {
    |  board: Board
    |}
    |
    |type Board {
    |  id: ID
    |  title: String
    |  issue: Issue
    |  @hydrated(
    |    service: "issues"
    |    field: "issue"
    |    arguments: [{name: "id" value: "${'$'}source.issueId"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  board: Board
    |}
    |
    |type Board {
    |  id: ID
    |  title: String
    |  issueId: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("board") { env ->
          Boards_Board(issueId = "ISSUE-1")}
      }
    }
    )
, Service(name="issues", overallSchema="""
    |type Query {
    |  issue(id: ID!): Issue
    |}
    |
    |type Issue {
    |  id: ID!
    |  cloudId: ID!
    |
    |  comments(first: Int = 20): CommentConnection
    |  @hydrated(
    |    service: "comments"
    |    field: "comments"
    |    arguments: [
    |      {name: "cloudId" value: "${'$'}source.cloudId"}
    |      {name: "first" value: "${'$'}argument.first"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issue(id: ID!): Issue
    |}
    |
    |type Issue {
    |  id: ID!
    |  cloudId: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          if (env.getArgument<Any?>("id") == "ISSUE-1") {
            Issues_Issue(cloudId = "CLOUD_ID-1")}
          else {
            null}
        }
      }
    }
    )
, Service(name="comments", overallSchema="""
    |type Query {
    |  comments(cloudId: ID!, first: Int): CommentConnection
    |}
    |
    |type CommentConnection {
    |  totalCount: Int
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  comments(cloudId: ID!, first: Int): CommentConnection
    |}
    |
    |type CommentConnection {
    |  totalCount: Int
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("comments") { env ->
          if (env.getArgument<Any?>("cloudId") == "CLOUD_ID-1" && env.getArgument<Any?>("first") ==
              10) {
            Comments_CommentConnection(totalCount = 10)}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Boards_Board(
    public val id: String? = null,
    public val title: String? = null,
    public val issueId: String? = null,
  )

  private data class Issues_Issue(
    public val id: String? = null,
    public val cloudId: String? = null,
  )

  private data class Comments_CommentConnection(
    public val totalCount: Int? = null,
  )
}
