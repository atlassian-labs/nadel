package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `complex identified by with some null source ids` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authors {
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
                  name: String
                  siteId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("users") { env ->
                        if (env.getArgument<Any?>("id") ==
                            listOf(
                                mapOf("userId" to "USER-2", "site" to "hello"),
                                mapOf("userId" to "USER-3", "site" to "hello"),
                                mapOf("userId" to "USER-2", "site" to "jdog"),
                            )
                        ) {
                            listOf(
                                UserService_User(id = "USER-2", name = "H-Two", siteId = "hello"),
                                UserService_User(id = "USER-3", name = "H-Three", siteId = "hello"),
                                UserService_User(id = "USER-2", name = "J-Two", siteId = "jdog"),
                            )
                        } else if (env.getArgument<Any?>("id") ==
                            listOf(
                                mapOf("userId" to "USER-5", "site" to "hello"),
                            )
                        ) {
                            listOf(UserService_User(id = "USER-5", name = "H-Five", siteId = "hello"))
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
                  authors: [User] @hydrated(
                    field: "users"
                    arguments: [{name: "id" value: "${'$'}source.authorIds"}]
                    inputIdentifiedBy: [
                      {sourceId: "authorIds.userId" resultId: "id"}
                      {sourceId: "authorIds.site" resultId: "siteId"}
                    ]
                    batchSize: 3
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
                  authorIds: [UserRef]
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issues") { env ->
                        listOf(
                            Issues_Issue(
                                authorIds = listOf(
                                    null,
                                    Issues_UserRef(
                                        userId = "USER-2",
                                        site = "hello",
                                    ),
                                ),
                                id = "ISSUE-1",
                            ),
                            Issues_Issue(
                                authorIds = listOf(
                                    Issues_UserRef(
                                        userId = "USER-3",
                                        site = "hello",
                                    ),
                                ),
                                id = "ISSUE-2",
                            ),
                            Issues_Issue(
                                authorIds = listOf(
                                    Issues_UserRef(userId = "USER-2", site = "jdog"),
                                    null,
                                    Issues_UserRef(
                                        userId = "USER-5",
                                        site = "hello",
                                    ),
                                ),
                                id = "ISSUE-3",
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
        val siteId: String? = null,
    )

    private data class UserService_UserInput(
        val userId: String? = null,
        val site: String? = null,
    )

    private data class Issues_Issue(
        val authorIds: List<Issues_UserRef?>? = null,
        val id: String? = null,
    )

    private data class Issues_UserRef(
        val userId: String? = null,
        val site: String? = null,
    )
}
