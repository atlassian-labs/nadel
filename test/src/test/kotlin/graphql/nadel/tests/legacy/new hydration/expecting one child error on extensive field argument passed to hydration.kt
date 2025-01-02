package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `expecting one child error on extensive field argument passed to hydration` : NadelLegacyIntegrationTest(
    query = """
        query {
          board(id: 1) {
            id
            cardChildren {
              id
              assignee {
                accountId
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "TestBoard",
            overallSchema = """
                type Query {
                  board(id: ID): SoftwareBoard
                }
                type SoftwareBoard @renamed(from: "Board") {
                  id: ID
                  cardChildren: [SoftwareCard] @renamed(from: "issueChildren")
                }
                type SoftwareCard @renamed(from: "Card") {
                  id: ID
                  assignee: User
                  @hydrated(
                    service: "Users"
                    field: "users"
                    arguments: [{name: "accountIds" value: "${'$'}source.issue.assignee.accountId"}]
                    identifiedBy: "accountId"
                    batchSize: 3
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Board {
                  id: ID
                  issueChildren: [Card]
                }
                type Card {
                  id: ID
                  issue: Issue
                }
                type Issue {
                  assignee: TestUser
                  id: ID
                }
                type Query {
                  board(id: ID): Board
                }
                type TestUser {
                  accountId: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("board") { env ->
                        if (env.getArgument<Any?>("id") == "1") {
                            TestBoard_Board(
                                id = "1",
                                issueChildren =
                                listOf(
                                    TestBoard_Card(
                                        id = "a1",
                                        issue =
                                        TestBoard_Issue(assignee = TestBoard_TestUser(accountId = "1")),
                                    ),
                                    TestBoard_Card(
                                        id =
                                        "a2",
                                        issue = TestBoard_Issue(assignee = TestBoard_TestUser(accountId = "2")),
                                    ),
                                    TestBoard_Card(
                                        id = "a3",
                                        issue =
                                        TestBoard_Issue(
                                            assignee =
                                            TestBoard_TestUser(accountId = "3"),
                                        ),
                                    ),
                                ),
                            )
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "Users",
            overallSchema = """
                type Query {
                  users(accountIds: [ID]): [User]
                }
                type User {
                  accountId: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  users(accountIds: [ID]): [User]
                }
                type User {
                  accountId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val usersById = listOf(
                        Users_User(accountId = "1"),
                        Users_User(accountId = "2"),
                        Users_User(accountId = "3"),
                    ).associateBy { it.accountId }

                    type.dataFetcher("users") { env ->
                        env.getArgument<List<String>>("accountIds")?.map(usersById::get)
                    }
                }
            },
        ),
    ),
) {
    private data class TestBoard_Board(
        val id: String? = null,
        val issueChildren: List<TestBoard_Card?>? = null,
    )

    private data class TestBoard_Card(
        val id: String? = null,
        val issue: TestBoard_Issue? = null,
    )

    private data class TestBoard_Issue(
        val assignee: TestBoard_TestUser? = null,
        val id: String? = null,
    )

    private data class TestBoard_TestUser(
        val accountId: String? = null,
    )

    private data class Users_User(
        val accountId: String? = null,
    )
}
