package graphql.nadel.tests.legacy.`new hydration`.index

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration matching using index one batch returns errors` : NadelLegacyIntegrationTest(
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
                        UserService_User(
                            id = "1",
                            name = "User-1",
                        ),
                    ).associateBy { it.id }

                    type.dataFetcher("usersByIds") { env ->
                        val ids = env.getArgument<List<String>>("ids")
                        if (ids?.contains("4") == true) {
                            DataFetcherResult
                                .newResult<Any>()
                                .data(null)
                                .errors(
                                    listOf(
                                        toGraphQLError(
                                            mapOf(
                                                "message"
                                                    to "Fail",
                                            ),
                                        ),
                                    ),
                                ).build()
                        } else {
                            ids?.map(usersByIds::get)
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
                  authors: [User]
                  @hydrated(
                    service: "UserService"
                    field: "usersByIds"
                    arguments: [{name: "ids" value: "${'$'}source.authorIds"}]
                    indexed: true
                    batchSize: 2
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
                                authorIds =
                                listOf("1", "2"),
                                id = "ISSUE-2",
                            ),
                            Issues_Issue(
                                authorIds = listOf("2", "4"),
                                id =
                                "ISSUE-3",
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
}
