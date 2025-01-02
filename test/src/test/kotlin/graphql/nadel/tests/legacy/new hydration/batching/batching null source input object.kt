package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `batching null source input object` : NadelLegacyIntegrationTest(query = """
|{
|  activity {
|    content {
|      __typename
|      ... on Issue {
|        id
|        title
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="activity",
    overallSchema="""
    |type Query {
    |  activity: [Activity]
    |}
    |type Activity {
    |  id: ID!
    |  content: Issue
    |    @hydrated(
    |      service: "issues"
    |      field: "issuesByIds"
    |      arguments: [
    |        {name: "ids" value: "${'$'}source.reference.issueId"}
    |      ]
    |    )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  activity: [Activity]
    |}
    |type Activity {
    |  id: ID!
    |  reference: ActivityReference
    |}
    |type ActivityReference {
    |  issueId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("activity") { env ->
          listOf(Activity_Activity(reference = null))}
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
    }
    )
)) {
  private data class Activity_Activity(
    public val id: String? = null,
    public val reference: Activity_ActivityReference? = null,
  )

  private data class Activity_ActivityReference(
    public val issueId: String? = null,
  )

  private data class Issues_Issue(
    public val id: String? = null,
    public val title: String? = null,
  )
}
