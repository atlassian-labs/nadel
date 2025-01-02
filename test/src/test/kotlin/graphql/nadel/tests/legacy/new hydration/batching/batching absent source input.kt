package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `batching absent source input` : NadelLegacyIntegrationTest(query = """
|{
|  activity {
|    content {
|      __typename
|      ... on Issue {
|        id
|        title
|      }
|      ... on Comment {
|        id
|        content
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="activity",
    overallSchema="""
    |type Query {
    |  activity: [Activity]
    |}
    |union ActivityContent = Issue | Comment
    |type Activity {
    |  id: ID!
    |  contentId: ID
    |  content: ActivityContent
    |    @hydrated(
    |      service: "comments"
    |      field: "commentsByIds"
    |      arguments: [
    |        {name: "ids" value: "${'$'}source.contentId"}
    |      ]
    |    )
    |    @hydrated(
    |      service: "issues"
    |      field: "issuesByIds"
    |      arguments: [
    |        {name: "ids" value: "${'$'}source.contentId"}
    |      ]
    |    )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  activity: [Activity]
    |}
    |type Activity {
    |  id: ID!
    |  contentId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("activity") { env ->
          listOf(Activity_Activity(contentId = null), Activity_Activity(contentId = ""),
              Activity_Activity(contentId = "comment/9001"), Activity_Activity(contentId =
              "issue/1234"))}
      }
    }
    )
, Service(name="issues", overallSchema="""
    |type Query {
    |  issuesByIds(ids: [ID!]!): [Issue!]
    |}
    |type Issue {
    |  id: ID!
    |  title: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issuesByIds(ids: [ID!]!): [Issue!]
    |}
    |type Issue {
    |  id: ID!
    |  title: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issuesByIds") { env ->
          if (env.getArgument<Any?>("ids") == listOf("issue/1234")) {
            listOf(Issues_Issue(id = "issue/1234", title = "One Two Three Four"))}
          else {
            null}
        }
      }
    }
    )
, Service(name="comments", overallSchema="""
    |type Query {
    |  commentsByIds(ids: [ID!]!): [Comment!]
    |}
    |type Comment {
    |  id: ID!
    |  content: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  commentsByIds(ids: [ID!]!): [Comment!]
    |}
    |type Comment {
    |  id: ID!
    |  content: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("commentsByIds") { env ->
          if (env.getArgument<Any?>("ids") == listOf("comment/9001")) {
            listOf(Comments_Comment(content = "It's over 9000", id = "comment/9001"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Activity_Activity(
    public val id: String? = null,
    public val contentId: String? = null,
  )

  private data class Issues_Issue(
    public val id: String? = null,
    public val title: String? = null,
  )

  private data class Comments_Comment(
    public val id: String? = null,
    public val content: String? = null,
  )
}
