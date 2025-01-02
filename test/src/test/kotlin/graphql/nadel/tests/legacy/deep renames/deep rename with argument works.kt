package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `deep rename with argument works` : NadelLegacyIntegrationTest(query = """
|query {
|  issue {
|    name(userId: "USER-01")
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type Query {
    |  issue: Issue
    |}
    |type Issue {
    |  name(userId: ID!): String @renamed(from: "detail.detailName")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  detail: IssueDetails
    |}
    |
    |type IssueDetails {
    |  detailName(userId: ID!): String
    |}
    |
    |type Query {
    |  issue: Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          IssueService_Issue(detail = IssueService_IssueDetails(detailName = "My Issue"))}
      }
    }
    )
)) {
  private data class IssueService_Issue(
    public val detail: IssueService_IssueDetails? = null,
  )

  private data class IssueService_IssueDetails(
    public val detailName: String? = null,
  )
}
