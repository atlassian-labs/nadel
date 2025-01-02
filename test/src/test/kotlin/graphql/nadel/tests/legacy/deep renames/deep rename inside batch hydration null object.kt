package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `deep rename inside batch hydration null object` : NadelLegacyIntegrationTest(query =
    """
|query {
|  users {
|    issue {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type Query {
    |  issuesByIds(id: [ID!]): [Issue]
    |}
    |type Issue {
    |  name: String @renamed(from: "detail.detailName")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID
    |  detail: IssueDetails
    |}
    |
    |type IssueDetails {
    |  detailName: String
    |}
    |
    |type Query {
    |  issuesByIds(id: [ID!]): [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issuesByIds") { env ->
          if (env.getArgument<Any?>("id") == listOf("issue-1", "issue-2", "issue-3")) {
            listOf(IssueService_Issue(detail = IssueService_IssueDetails(detailName =
                "Memes are the DNA of the soul"), id = "issue-1"), IssueService_Issue(detail =
                IssueService_IssueDetails(detailName = "Names are arbitrary"), id = "issue-3"),
                null)}
          else {
            null}
        }
      }
    }
    )
, Service(name="UserService", overallSchema="""
    |type Query {
    |  users: [User]
    |}
    |type User {
    |  issueId: ID
    |  issue: Issue @hydrated(
    |    service: "IssueService"
    |    field: "issuesByIds"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.issueId"}
    |    ]
    |    identifiedBy: "id"
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  users: [User]
    |}
    |
    |type User {
    |  issueId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("users") { env ->
          listOf(UserService_User(issueId = "issue-1"), UserService_User(issueId = "issue-2"),
              UserService_User(issueId = "issue-3"))}
      }
    }
    )
)) {
  private data class IssueService_Issue(
    public val id: String? = null,
    public val detail: IssueService_IssueDetails? = null,
  )

  private data class IssueService_IssueDetails(
    public val detailName: String? = null,
  )

  private data class UserService_User(
    public val issueId: String? = null,
  )
}
