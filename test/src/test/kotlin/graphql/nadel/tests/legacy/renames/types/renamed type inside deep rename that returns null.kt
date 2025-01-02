package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `renamed type inside deep rename that returns null` : NadelLegacyIntegrationTest(query
    = """
|query {
|  issueById(id: "issue-1") {
|    assignee {
|      name
|      __typename
|      friends {
|        __typename
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
    |  assignee: IssueUser @renamed(from: "details.assignee")
    |}
    |type IssueUser @renamed(from: "User") {
    |  name: String
    |  friends: [IssueUser]
    |}
    |type IssueDetails @renamed(from: "Details") {
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID
    |  details: Details
    |}
    |type Details {
    |  name: String
    |  assignee: User
    |}
    |type User {
    |  name: String
    |  friends: [User]
    |}
    |type Query {
    |  issueById(id: ID!): Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issueById") { env ->
          if (env.getArgument<Any?>("id") == "issue-1") {
            IssueService_Issue(details = IssueService_Details(assignee = null))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class IssueService_Details(
    public val name: String? = null,
    public val assignee: IssueService_User? = null,
  )

  private data class IssueService_Issue(
    public val id: String? = null,
    public val details: IssueService_Details? = null,
  )

  private data class IssueService_User(
    public val name: String? = null,
    public val friends: List<IssueService_User?>? = null,
  )
}
