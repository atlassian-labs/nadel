package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `deep rename inside hydration` : NadelLegacyIntegrationTest(query = """
|query {
|  me {
|    issue {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type Query {
    |  issueById(id: ID!): Issue
    |}
    |type Issue {
    |  name: String @renamed(from: "detail.detailName")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  detail: IssueDetails
    |}
    |
    |type IssueDetails {
    |  detailName: String
    |}
    |
    |type Query {
    |  issueById(id: ID!): Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issueById") { env ->
          if (env.getArgument<Any?>("id") == "issue-1") {
            IssueService_Issue(detail = IssueService_IssueDetails(detailName = "Detail-1"))}
          else {
            null}
        }
      }
    }
    )
, Service(name="UserService", overallSchema="""
    |type Query {
    |  me: User
    |}
    |type User {
    |  issueId: ID
    |  issue: Issue @hydrated(
    |    service: "IssueService"
    |    field: "issueById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.issueId"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  me: User
    |}
    |type User {
    |  issueId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("me") { env ->
          UserService_User(issueId = "issue-1")}
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

  private data class UserService_User(
    public val issueId: String? = null,
  )
}
