package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `renamed type inside hydration` : NadelLegacyIntegrationTest(query = """
|query {
|  me {
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
    |  issueById(id: ID!): Issue
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
    |  issueById(id: ID!): Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issueById") { env ->
          if (env.getArgument<Any?>("id") == "issue-2") {
            IssueService_Issue(details = IssueService_Details(name = "Issue two"))}
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
    |
    |type User {
    |  issueId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("me") { env ->
          UserService_User(issueId = "issue-2")}
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
