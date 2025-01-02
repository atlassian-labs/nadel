package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `batching multiple source ids going to different services` :
    NadelLegacyIntegrationTest(query = """
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
    |  contentIds: [ID!]!
    |  content: [ActivityContent]
    |  @hydrated(
    |    service: "comments"
    |    field: "commentsByIds"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.contentIds"}
    |    ]
    |  )
    |  @hydrated(
    |    service: "issues"
    |    field: "issuesByIds"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.contentIds"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  activity: [Activity]
    |}
    |type Activity {
    |  id: ID!
    |  contentIds: [ID!]!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("activity") { env ->
          listOf(Activity_Activity(contentIds = listOf("issue/4000", "comment/5000",
              "comment/6000")), Activity_Activity(contentIds = listOf("issue/8080")),
              Activity_Activity(contentIds = listOf("issue/7496", "comment/9001")),
              Activity_Activity(contentIds = listOf("issue/1234", "comment/1234")))}
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
          if (env.getArgument<Any?>("ids") == listOf("issue/4000", "issue/8080", "issue/7496",
              "issue/1234")) {
            listOf(Issues_Issue(id = "issue/4000", title = "Four Thousand"), Issues_Issue(id =
                "issue/8080", title = "Eighty Eighty"), Issues_Issue(id = "issue/7496", title =
                "Seven Four Nine Six"), Issues_Issue(id = "issue/1234", title =
                "One Two Three Four"))}
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
          if (env.getArgument<Any?>("ids") == listOf("comment/5000", "comment/6000", "comment/9001",
              "comment/1234")) {
            listOf(Comments_Comment(content = "Five Thousand", id = "comment/5000"),
                Comments_Comment(content = "Six Thousand", id = "comment/6000"),
                Comments_Comment(content = "It's over 9000", id = "comment/9001"),
                Comments_Comment(content = "One Two Three Four", id = "comment/1234"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Activity_Activity(
    public val id: String? = null,
    public val contentIds: List<String>? = null,
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
