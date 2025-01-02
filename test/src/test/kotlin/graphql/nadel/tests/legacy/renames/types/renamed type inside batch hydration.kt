package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `renamed type inside batch hydration` : NadelLegacyIntegrationTest(query = """
|query {
|  users {
|    issue {
|      details {
|        __typename
|        name
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type Query {
    |  issuesByIds(id: [ID!]): [Issue]
    |}
    |type Issue {
    |  details: IssueDetails
    |}
    |type IssueDetails @renamed(from: "Details") {
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID
    |  details: Details
    |}
    |
    |type Details {
    |  name: String
    |}
    |
    |type Query {
    |  issuesByIds(id: [ID!]): [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issuesByIds") { env ->
          if (env.getArgument<Any?>("id") == listOf("issue-1", "issue-2", "issue-3")) {
            listOf(IssueService_Issue(details = IssueService_Details(name = "Details of issue one"),
                id = "issue-1"), IssueService_Issue(details = IssueService_Details(name =
                "Issue two"), id = "issue-2"), IssueService_Issue(details =
                IssueService_Details(name = "Issue four – no wait three"), id = "issue-3"))}
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
  private data class IssueService_Details(
    public val name: String? = null,
  )

  private data class IssueService_Issue(
    public val id: String? = null,
    public val details: IssueService_Details? = null,
  )

  private data class UserService_User(
    public val issueId: String? = null,
  )
}
