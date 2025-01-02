package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `deep rename inside deep rename` : NadelLegacyIntegrationTest(query = """
|query {
|  issue {
|    extras {
|      ownerName
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  issue: JiraIssue @renamed(from: "first")
    |}
    |type JiraIssue @renamed(from: "Issue") {
    |  extras: IssueExtra @renamed(from: "details.extras")
    |}
    |type IssueExtra {
    |  ownerName: String @renamed(from: "owner.name")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  first: Issue
    |}
    |type Issue {
    |  details: IssueDetail
    |}
    |type IssueDetail {
    |  extras: IssueExtra
    |}
    |type IssueExtra {
    |  owner: User
    |}
    |type User {
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("first") { env ->
          Issues_Issue(details = Issues_IssueDetail(extras = Issues_IssueExtra(owner =
              Issues_User(name = "Franklin"))))}
      }
    }
    )
)) {
  private data class Issues_Issue(
    public val details: Issues_IssueDetail? = null,
  )

  private data class Issues_IssueDetail(
    public val extras: Issues_IssueExtra? = null,
  )

  private data class Issues_IssueExtra(
    public val owner: Issues_User? = null,
  )

  private data class Issues_User(
    public val name: String? = null,
  )
}
