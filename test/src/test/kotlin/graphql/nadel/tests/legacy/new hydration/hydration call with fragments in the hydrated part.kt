package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration call with fragments in the hydrated part` : NadelLegacyIntegrationTest(
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
          usersByIds(id: ["USER-1"]) {
            ...UserFragment1
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
                  usersByIds(id: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  usersByIds(id: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("usersByIds") { env ->
                        if (env.getArgument<Any?>("id") == listOf("USER-1")) {
                            listOf(UserService_User(id = "USER-1", name = "User 1"))
                        } else if (env.getArgument<Any?>("id") == listOf("USER-1", "USER-2")) {
                            listOf(
                                UserService_User(id = "USER-1", name = "User 1"),
                                UserService_User(
                                    id = "USER-2",
                                    name = "User 2",
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
                    field: "usersByIds"
                    arguments: [{name: "id" value: "${'$'}source.authorDetails.authorId"}]
                    identifiedBy: "id"
                    batchSize: 2
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
                                authorDetails =
                                listOf(
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

    private data class Issues_AuthorDetail(
        val authorId: String? = null,
        val name: String? = null,
    )

    private data class Issues_Issue(
        val authorDetails: List<Issues_AuthorDetail?>? = null,
        val id: String? = null,
    )
}
