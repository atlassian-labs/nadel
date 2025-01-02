package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `index hydration with partitioned inputs` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authors {
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
                  usersByIds(id: [ID]): [User]
                }
                type User {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  usersByIds(id: [ID]): [User]
                }
                type User {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val usersById = listOf(
                        UserService_User(id = "site-1/user-1"),
                        UserService_User(id = "site-1/user-3"),
                        UserService_User(id = "site-1/user-5"),
                        UserService_User(id = "site-2/user-2"),
                        UserService_User(id = "site-2/user-4"),
                    ).associateBy { it.id }

                    type.dataFetcher("usersByIds") { env ->
                        val ids = env.getArgument<List<String>>("id")
                        if (ids != null) {
                            val uniqueCloudIds = ids.mapTo(mutableSetOf()) { it.substringBefore("/") }
                            if (uniqueCloudIds.size > 1) {
                                throw IllegalArgumentException("Cannot query multiple cloud IDs at once")
                            }
                        }
                        ids?.map(usersById::get)
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
                  authors: [User]
                  @hydrated(
                    service: "UserService"
                    field: "usersByIds"
                    arguments: [{name: "id" value: "${'$'}source.authorIds"}]
                    identifiedBy: "id"
                    batchSize: 2
                    indexed: true
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  authorIds: [ID]
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
                            Issues_Issue(authorIds = listOf("site-1/user-1", "site-2/user-2"), id = "ISSUE-1"),
                            Issues_Issue(authorIds = listOf("site-1/user-3"), id = "ISSUE-2"),
                            Issues_Issue(authorIds = listOf("site-2/user-4", "site-1/user-5"), id = "ISSUE-3"),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class UserService_User(
        val id: String? = null,
    )

    private data class Issues_Issue(
        val authorIds: List<String?>? = null,
        val id: String? = null,
    )
}
