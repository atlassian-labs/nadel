package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `two deep renames` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authorId
            authorName
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID
                  authorId: ID @renamed(from: "authorDetails.authorId")
                  authorName: String @renamed(from: "authorDetails.name")
                }
            """.trimIndent(),
            underlyingSchema = """
                type AuthorDetail {
                  authorId: ID
                  name: String
                }
                type Issue {
                  authorDetails: AuthorDetail
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
                                authorDetails = Issues_AuthorDetail(
                                    authorId = "USER-1",
                                    name = "User 1",
                                ),
                                id = "ISSUE-1",
                            ),
                            Issues_Issue(
                                authorDetails = Issues_AuthorDetail(
                                    authorId = "USER-2",
                                    name = "User 2",
                                ),
                                id = "ISSUE-2",
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Issues_AuthorDetail(
        val authorId: String? = null,
        val name: String? = null,
    )

    private data class Issues_Issue(
        val authorDetails: Issues_AuthorDetail? = null,
        val id: String? = null,
    )
}
