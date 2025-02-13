package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration call with fragments in the hydrated part and synthetic field` : NadelLegacyIntegrationTest(
    query = """
        fragment IssueFragment on Issue {
          id
        }
        query {
          issues {
            ...IssueFragment
            id
            authors {
              id
              ...UserFragment1
            }
          }
          userQuery {
            usersByIds(id: ["USER-1"]) {
              ...UserFragment1
            }
          }
        }
        fragment UserFragment1 on User {
          id
          name
          ...UserFragment2
        }
        fragment UserFragment2 on User {
          name
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "UserService",
            overallSchema = """
                type Query {
                  userQuery: UserQuery
                }
                type UserQuery {
                  usersByIds(id: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  userQuery: UserQuery
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
                    type.dataFetcher("userQuery") {
                        Unit
                    }
                }
                wiring.type("UserQuery") { type ->
                    val usersById = listOf(
                        UserService_User(id = "USER-1", name = "User 1"),
                        UserService_User(id = "USER-2", name = "User 2"),
                    ).associateBy { it.id }

                    type.dataFetcher("usersByIds") { env ->
                        env.getArgument<List<String>>("id")?.map(usersById::get)
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
                  authorDetails: [AuthorDetail]
                  authors: [User]
                  @hydrated(
                    service: "UserService"
                    field: "userQuery.usersByIds"
                    arguments: [{name: "id" value: "${'$'}source.authorDetails.authorId"}]
                    identifiedBy: "id"
                  )
                }
                type AuthorDetail {
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type AuthorDetail {
                  authorId: ID
                  name: String
                }
                type Issue {
                  authorDetails: [AuthorDetail]
                  id: ID
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
                                authorDetails = listOf(
                                    Issues_AuthorDetail(authorId = "USER-1"),
                                    Issues_AuthorDetail(authorId = "USER-2"),
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

    private data class Issues_AuthorDetail(
        val authorId: String? = null,
        val name: String? = null,
    )

    private data class Issues_Issue(
        val authorDetails: List<Issues_AuthorDetail?>? = null,
        val id: String? = null,
    )
}
