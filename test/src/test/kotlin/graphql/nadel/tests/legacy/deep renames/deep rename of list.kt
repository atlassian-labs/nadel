package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `deep rename of list` : NadelLegacyIntegrationTest(query = """
|query {
|  details {
|    labels
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  details: [IssueDetail]
    |}
    |type IssueDetail {
    |  labels: [String] @renamed(from: "issue.labels")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  labels: [String]
    |}
    |
    |type IssueDetail {
    |  issue: Issue
    |}
    |
    |type Query {
    |  details: [IssueDetail]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("details") { env ->
          listOf(Issues_IssueDetail(issue = Issues_Issue(labels = listOf("label1", "label2"))))}
      }
    }
    )
)) {
  private data class Issues_Issue(
    public val labels: List<String?>? = null,
  )

  private data class Issues_IssueDetail(
    public val issue: Issues_Issue? = null,
  )
}
