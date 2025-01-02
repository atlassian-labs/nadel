package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `batching conditional hydration in abstract type` : NadelLegacyIntegrationTest(query =
    """
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
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="monolith",
    overallSchema="""
    |type Query {
    |  activity: [IActivity]
    |  issuesByIds(ids: [ID!]!): [Issue!]
    |  commentsByIds(ids: [ID!]!): [Comment!]
    |}
    |interface IActivity {
    |  content: [ActivityContent]
    |}
    |union ActivityContent = Issue | Comment
    |type Activity implements IActivity {
    |  id: ID!
    |  content: [ActivityContent]
    |  @hydrated(
    |    service: "monolith"
    |    field: "commentsByIds"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.contentIds"}
    |    ]
    |  )
    |  @hydrated(
    |    service: "monolith"
    |    field: "issuesByIds"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.contentIds"}
    |    ]
    |  )
    |}
    |type SingleActivity implements IActivity {
    |  id: ID!
    |  content: [ActivityContent]
    |  @hydrated(
    |    service: "monolith"
    |    field: "issuesByIds"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.contentId"}
    |    ]
    |  )
    |}
    |type Issue {
    |  id: ID!
    |  title: String
    |}
    |type Comment {
    |  id: ID!
    |  content: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  activity: [IActivity]
    |  commentsByIds(ids: [ID!]!): [Comment!]
    |  issuesByIds(ids: [ID!]!): [Issue!]
    |}
    |interface IActivity {
    |  content: [ActivityContent]
    |}
    |union ActivityContent = Issue | Comment
    |type Activity implements IActivity {
    |  id: ID!
    |  content: [ActivityContent]
    |  contentIds: [ID!]
    |}
    |type SingleActivity implements IActivity {
    |  id: ID!
    |  content: [ActivityContent]
    |  contentId: ID!
    |}
    |type Issue {
    |  id: ID!
    |  title: String
    |}
    |type Comment {
    |  id: ID!
    |  content: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("activity") { env ->
          listOf(Monolith_Activity(contentIds = listOf("issue/4000", "comment/5000",
              "comment/6000")), Monolith_SingleActivity(contentId = "issue/8080"),
              Monolith_Activity(contentIds = listOf("comment/1234", "comment/9001")),
              Monolith_SingleActivity(contentId = "issue/7496"))}

        .dataFetcher("issuesByIds") { env ->
          if (env.getArgument<Any?>("ids") == listOf("issue/4000")) {
            listOf(Monolith_Issue(id = "issue/4000", title = "Four Thousand"))}
          else if (env.getArgument<Any?>("ids") == listOf("issue/8080", "issue/7496")) {
            listOf(Monolith_Issue(id = "issue/7496", title = "Seven Four Nine Six"))}
          else {
            null}
        }

        .dataFetcher("commentsByIds") { env ->
          if (env.getArgument<Any?>("ids") == listOf("comment/5000", "comment/6000", "comment/1234",
              "comment/9001")) {
            listOf(Monolith_Comment(content = "Five Thousand", id = "comment/5000"),
                Monolith_Comment(content = "Six Thousand", id = "comment/6000"),
                Monolith_Comment(content = "It's over 9000", id = "comment/9001"),
                Monolith_Comment(content = "One Two Three Four", id = "comment/1234"))}
          else {
            null}
        }
      }
      wiring.type("ActivityContent") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }

      wiring.type("IActivity") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
)) {
  private data class Monolith_Activity(
    public val id: String? = null,
    override val content: List<Monolith_ActivityContent?>? = null,
    public val contentIds: List<String>? = null,
  ) : Monolith_IActivity

  private sealed interface Monolith_ActivityContent

  private data class Monolith_Comment(
    public val id: String? = null,
    public val content: String? = null,
  ) : Monolith_ActivityContent

  private interface Monolith_IActivity {
    public val content: List<Monolith_ActivityContent?>?
  }

  private data class Monolith_Issue(
    public val id: String? = null,
    public val title: String? = null,
  ) : Monolith_ActivityContent

  private data class Monolith_SingleActivity(
    public val id: String? = null,
    override val content: List<Monolith_ActivityContent?>? = null,
    public val contentId: String? = null,
  ) : Monolith_IActivity
}
