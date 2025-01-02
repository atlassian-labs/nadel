package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `rename with first path element returning null` : NadelLegacyIntegrationTest(query =
    """
|query {
|  issue {
|    name
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  issue: Issue
    |}
    |type Issue {
    |  name: String @renamed(from: "details.name")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  details: IssueDetails
    |}
    |
    |type IssueDetails {
    |  name: String
    |}
    |
    |type Query {
    |  issue: Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          Issues_Issue(details = null)}
      }
    }
    )
)) {
  private data class Issues_Issue(
    public val details: Issues_IssueDetails? = null,
  )

  private data class Issues_IssueDetails(
    public val name: String? = null,
  )
}
