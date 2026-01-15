package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `one synthetic hydration call with longer path arguments and merged fields and renamed type` :
    NadelLegacyIntegrationTest(
        query = """
            query {
              issues {
                id
                authors {
                  name
                  id
                }
              }
            }
        """.trimIndent(),
        variables = emptyMap(),
        services = listOf(
            Service(
                name = "UserService",
                overallSchema = """
                    type Query {
                      usersQuery: RenamedUserQuery
                    }
                    type RenamedUserQuery @renamed(from: "UserQuery") {
                      usersByIds(id: [ID]): [RenamedUser]
                    }
                    type RenamedUser @renamed(from: "User") {
                      id: ID
                      name: String
                    }
                """.trimIndent(),
                underlyingSchema = """
                    type Query {
                      usersQuery: UserQuery
                    }
                    type User {
                      id: ID
                      name: String
                    }
                    type UserQuery {
                      usersByIds(id: [ID]): [User]
                    }
                """.trimIndent(),
                runtimeWiring = { wiring ->
                    wiring.type("Query") { type ->
                        type.dataFetcher("usersQuery") {
                        }
                    }
                    wiring.type("UserQuery") { type ->
                        val usersByIds = listOf(
                            UserService_User(id = "USER-1", name = "User 1"),
                            UserService_User(id = "USER-2", name = "User 2"),
                        ).associateBy { it.id }

                        type.dataFetcher("usersByIds") { env ->
                            env.getArgument<List<String>>("id")?.map(usersByIds::get)
                        }
                    }
                },
            ),
            Service(
                name = "Issues",
                overallSchema = """
                    type Query {
                      issues: [Issue]
                    }
                    type Issue {
                      id: ID
                      authors: [RenamedUser]
                      @hydrated(
                        field: "usersQuery.usersByIds"
                        arguments: [{name: "id" value: "${'$'}source.authors.authorId"}]
                        identifiedBy: "id"
                        batchSize: 2
                      )
                    }
                """.trimIndent(),
                underlyingSchema = """
                    type Issue {
                      authorIds: [ID]
                      authors: [IssueUser]
                      id: ID
                    }
                    type IssueUser {
                      authorId: ID
                    }
                    type Query {
                      issues: [Issue]
                    }
                """.trimIndent(),
                runtimeWiring = { wiring ->
                    wiring.type("Query") { type ->
                        type.dataFetcher("issues") { env ->
                            listOf(
                                Issues_Issue(
                                    authors = listOf(
                                        Issues_IssueUser(authorId = "USER-1"),
                                        Issues_IssueUser(authorId = "USER-2"),
                                    ),
                                    id = "ISSUE-1",
                                ),
                            )
                        }
                    }
                },
            ),
        ),
    ) {
    private data class UserService_User(
        val id: String? = null,
        val name: String? = null,
    )

    private data class UserService_UserQuery(
        val usersByIds: List<UserService_User?>? = null,
    )

    private data class Issues_Issue(
        val authorIds: List<String?>? = null,
        val authors: List<Issues_IssueUser?>? = null,
        val id: String? = null,
    )

    private data class Issues_IssueUser(
        val authorId: String? = null,
    )
}
