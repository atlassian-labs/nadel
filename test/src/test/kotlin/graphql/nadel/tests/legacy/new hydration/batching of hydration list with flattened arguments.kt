package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching of hydration list with flattened arguments` : NadelLegacyIntegrationTest(
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
                    val usersByIds = listOf(
                        UserService_User(id = "USER-1"),
                        UserService_User(id = "USER-2"),
                        UserService_User(id = "USER-3"),
                        UserService_User(id = "USER-4"),
                        UserService_User(id = "USER-5"),
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
                  authors: [User]
                  @hydrated(
                    service: "UserService"
                    field: "usersByIds"
                    arguments: [{name: "id" value: "${'$'}source.authors.authorId"}]
                    identifiedBy: "id"
                    batchSize: 3
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
                            Issues_Issue(
                                authors = listOf(Issues_IssueUser(authorId = "USER-3")),
                                id = "ISSUE-2",
                            ),
                            Issues_Issue(
                                authors = listOf(
                                    Issues_IssueUser(authorId = "USER-2"),
                                    Issues_IssueUser(authorId = "USER-4"),
                                    Issues_IssueUser(authorId = "USER-5"),
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
