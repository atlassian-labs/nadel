package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `renamed type inside list` : NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    __typename
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type Query {
    |  issues: [JiraIssue]
    |}
    |type JiraIssue @renamed(from: "Issue") {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issues: [Issue]
    |}
    |type Issue {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(IssueService_Issue(), null, IssueService_Issue(), IssueService_Issue())}
      }
    }
    )
)) {
  private data class IssueService_Issue(
    public val id: String? = null,
  )
}
