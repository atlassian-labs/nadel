package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `expecting one child error on extensive field argument passed to synthetic hydration` :
    NadelLegacyIntegrationTest(query = """
|query {
|  board(id: 1) {
|    id
|    cardChildren {
|      assignee {
|        accountId
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="TestBoard",
    overallSchema="""
    |type Query {
    |  board(id: ID): SoftwareBoard
    |}
    |type SoftwareBoard @renamed(from: "Board") {
    |  id: ID
    |  cardChildren: [SoftwareCard] @renamed(from: "issueChildren")
    |}
    |type SoftwareCard @renamed(from: "Card") {
    |  id: ID
    |  assignee: User
    |  @hydrated(
    |    service: "Users"
    |    field: "usersQuery.users"
    |    arguments: [{name: "accountIds" value: "${'$'}source.issue.assignee.accountId"}]
    |    identifiedBy: "accountId"
    |    batchSize: 3
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Board {
    |  id: ID
    |  issueChildren: [Card]
    |}
    |
    |type Card {
    |  id: ID
    |  issue: Issue
    |}
    |
    |type Issue {
    |  assignee: TestUser
    |  id: ID
    |}
    |
    |type Query {
    |  board(id: ID): Board
    |}
    |
    |type TestUser {
    |  accountId: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("board") { env ->
          if (env.getArgument<Any?>("id") == "1") {
            TestBoard_Board(id = "1", issueChildren = listOf(TestBoard_Card(issue =
                TestBoard_Issue(assignee = TestBoard_TestUser(accountId = "1"))),
                TestBoard_Card(issue = TestBoard_Issue(assignee = TestBoard_TestUser(accountId =
                "2"))), TestBoard_Card(issue = TestBoard_Issue(assignee =
                TestBoard_TestUser(accountId = "3")))))}
          else {
            null}
        }
      }
    }
    )
, Service(name="Users", overallSchema="""
    |type Query {
    |  usersQuery: UserQuery
    |}
    |type UserQuery {
    |  users(accountIds: [ID]): [User]
    |}
    |type User {
    |  accountId: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  usersQuery: UserQuery
    |}
    |
    |type User {
    |  accountId: ID
    |}
    |
    |type UserQuery {
    |  users(accountIds: [ID]): [User]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("usersQuery") {
          Unit}
      }
      wiring.type("UserQuery") { type ->
        type.dataFetcher("users") { env ->
          if (env.getArgument<Any?>("accountIds") == listOf("1", "2", "3")) {
            listOf(Users_User(accountId = "1"), Users_User(accountId = "2"), Users_User(accountId =
                "3"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class TestBoard_Board(
    public val id: String? = null,
    public val issueChildren: List<TestBoard_Card?>? = null,
  )

  private data class TestBoard_Card(
    public val id: String? = null,
    public val issue: TestBoard_Issue? = null,
  )

  private data class TestBoard_Issue(
    public val assignee: TestBoard_TestUser? = null,
    public val id: String? = null,
  )

  private data class TestBoard_TestUser(
    public val accountId: String? = null,
  )

  private data class Users_User(
    public val accountId: String? = null,
  )

  private data class Users_UserQuery(
    public val users: List<Users_User?>? = null,
  )
}
