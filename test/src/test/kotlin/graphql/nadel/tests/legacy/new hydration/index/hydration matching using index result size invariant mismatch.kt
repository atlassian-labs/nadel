package graphql.nadel.tests.legacy.`new hydration`.index

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration matching using index result size invariant mismatch` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authors {
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
                  usersByIds(ids: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  usersByIds(ids: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val usersByIds = listOf(
                        UserService_User(id = "1", name = "Name")
                    ).associateBy { it.id }

                    type.dataFetcher("usersByIds") { env ->
                        env.getArgument<List<String>>("ids")?.mapNotNull(usersByIds::get)
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
                    arguments: [{name: "ids" value: "${'$'}source.authorIds"}]
                    indexed: true
                    batchSize: 5
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
                            Issues_Issue(authorIds = listOf("1"), id = "ISSUE-1"),
                            Issues_Issue(
                                authorIds = listOf("1", "2"),
                                id = "ISSUE-2",
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

    private data class Issues_Issue(
        val authorIds: List<String?>? = null,
        val id: String? = null,
    )

    override fun assertFailure(e: Exception): Boolean = e.message ==
        "If you use indexed hydration then you MUST follow a contract where the resolved nodes matches the size of the input arguments"
}
