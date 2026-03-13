package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `complex identified by` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            author {
              id
              name
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
                  users(id: [UserInput]): [User]
                }
                input UserInput {
                  userId: ID
                  site: String
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  users(id: [UserInput]): [User]
                }
                input UserInput {
                  userId: ID
                  site: String
                }
                type User {
                  id: ID
                  siteId: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("users") { env ->
                        if (env.getArgument<Any?>("id") ==
                            listOf(
                                mapOf("userId" to "USER-5", "site" to "hello"),
                                mapOf("userId" to "USER-2", "site" to "hello"),
                            )
                        ) {
                            listOf(
                                UserService_User(id = "USER-5", name = "H-Five", siteId = "hello"),
                                UserService_User(id = "USER-2", name = "H-Two", siteId = "hello"),
                            )
                        } else if (env.getArgument<Any?>("id") ==
                            listOf(
                                mapOf(
                                    "userId" to "USER-1",
                                    "site" to "hello",
                                ),
                                mapOf("userId" to "USER-3", "site" to "hello"),
                                mapOf(
                                    "userId" to "USER-2",
                                    "site" to "jdog",
                                ),
                                mapOf("userId" to "USER-4", "site" to "hello"),
                            )
                        ) {
                            listOf(
                                UserService_User(id = "USER-1", name = "H-One", siteId = "hello"),
                                UserService_User(id = "USER-3", name = "H-Three", siteId = "hello"),
                                UserService_User(id = "USER-2", name = "J-Two", siteId = "jdog"),
                                UserService_User(id = "USER-4", name = "H-Four", siteId = "hello"),
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
                  author: User @hydrated(
                    field: "users"
                    arguments: [{name: "id" value: "${'$'}source.authorId"}]
                    inputIdentifiedBy: [
                      {sourceId: "authorId.userId" resultId: "id"}
                      {sourceId: "authorId.site" resultId: "siteId"}
                    ]
                    batchSize: 4
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issues: [Issue]
                }
                type UserRef {
                  userId: ID
                  site: String
                }
                type Issue {
                  authorId: UserRef
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issues") { env ->
                        listOf(
                            Issues_Issue(
                                authorId = Issues_UserRef(userId = "USER-1", site = "hello"),
                                id = "ISSUE-1",
                            ),
                            Issues_Issue(
                                authorId = Issues_UserRef(userId = "USER-3", site = "hello"),
                                id = "ISSUE-2",
                            ),
                            Issues_Issue(
                                authorId = Issues_UserRef(
                                    userId = "USER-2",
                                    site = "jdog",
                                ),
                                id = "ISSUE-3",
                            ),
                            Issues_Issue(
                                authorId = Issues_UserRef(
                                    userId = "USER-4",
                                    site = "hello",
                                ),
                                id = "ISSUE-4",
                            ),
                            Issues_Issue(
                                authorId = Issues_UserRef(
                                    userId = "USER-5",
                                    site = "hello",
                                ),
                                id = "ISSUE-5",
                            ),
                            Issues_Issue(
                                authorId = Issues_UserRef(userId = "USER-2", site = "jdog"),
                                id = "ISSUE-6",
                            ),
                            Issues_Issue(
                                authorId = Issues_UserRef(userId = "USER-2", site = "hello"),
                                id = "ISSUE-7",
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
        val siteId: String? = null,
        val name: String? = null,
    )

    private data class UserService_UserInput(
        val userId: String? = null,
        val site: String? = null,
    )

    private data class Issues_Issue(
        val authorId: Issues_UserRef? = null,
        val id: String? = null,
    )

    private data class Issues_UserRef(
        val userId: String? = null,
        val site: String? = null,
    )
}
