package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `batching single source id` : NadelLegacyIntegrationTest(query = """
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
    |  contentId: ID!
    |  content: ActivityContent
    |  @hydrated(
    |    service: "comments"
    |    field: "commentsByIds"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.contentId"}
    |    ]
    |  )
    |  @hydrated(
    |    service: "issues"
    |    field: "issuesByIds"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.contentId"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  activity: [Activity]
    |}
    |type Activity {
    |  id: ID!
    |  contentId: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("activity") { env ->
          listOf(Activity_Activity(contentId = "issue/4000"), Activity_Activity(contentId =
              "issue/8080"), Activity_Activity(contentId = "issue/7496"),
              Activity_Activity(contentId = "comment/1234"))}
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
          if (env.getArgument<Any?>("ids") == listOf("issue/4000", "issue/8080", "issue/7496")) {
            listOf(Issues_Issue(id = "issue/4000", title = "Four Thousand"), Issues_Issue(id =
                "issue/8080", title = "Eighty Eighty"), Issues_Issue(id = "issue/7496", title =
                "Seven Four Nine Six"))}
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
          if (env.getArgument<Any?>("ids") == listOf("comment/1234")) {
            listOf(Comments_Comment(content = "One Two Three Four", id = "comment/1234"))}
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
