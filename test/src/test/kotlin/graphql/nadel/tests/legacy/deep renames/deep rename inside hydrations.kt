package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `deep rename inside hydrations` : NadelLegacyIntegrationTest(query = """
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
          else if (env.getArgument<Any?>("id") == "issue-2") {
            IssueService_Issue(detail = IssueService_IssueDetails(detailName = "Detail-2"))}
          else if (env.getArgument<Any?>("id") == "issue-3") {
            IssueService_Issue(detail = IssueService_IssueDetails(detailName = "A name goes here"))}
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
    |    field: "issueById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.issueId"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  users: [User]
    |}
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
    public val detail: IssueService_IssueDetails? = null,
  )

  private data class IssueService_IssueDetails(
    public val detailName: String? = null,
  )

  private data class UserService_User(
    public val issueId: String? = null,
  )
}
