package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `typename is sent to owning service even when no fields are queried` : NadelLegacyIntegrationTest(
    query = """
        {
          issue {
            __typename
            search {
              id
              count
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                directive @namespaced on FIELD_DEFINITION
                type Query {
                  issue: IssueQuery @namespaced
                }
                type IssueQuery {
                  getIssue: Issue
                }
                type Issue {
                  id: ID
                  text: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue: IssueQuery
                }
                type IssueQuery {
                  getIssue: Issue
                }
                type Issue {
                  id: ID
                  text: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        Issues_IssueQuery()
                    }
                }
            },
        ),
        Service(
            name = "IssueSearch",
            overallSchema = """
                extend type IssueQuery {
                  search: SearchResult
                }
                type SearchResult {
                  id: ID
                  count: Int
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue: IssueQuery
                }
                type IssueQuery {
                  search: SearchResult
                }
                type SearchResult {
                  id: ID
                  count: Int
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        IssueSearch_IssueQuery(
                            search =
                            IssueSearch_SearchResult(
                                count = 1_001,
                                id =
                                "search-id",
                            ),
                        )
                    }
                }
            },
        ),
        Service(
            name = "IssueComments",
            overallSchema = """
                extend type IssueQuery {
                  commentsCount: Int
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue: IssueQuery
                }
                type IssueQuery {
                  commentsCount: Int
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class Issues_Issue(
        val id: String? = null,
        val text: String? = null,
    )

    private data class Issues_IssueQuery(
        val getIssue: Issues_Issue? = null,
    )

    private data class IssueSearch_IssueQuery(
        val search: IssueSearch_SearchResult? = null,
    )

    private data class IssueSearch_SearchResult(
        val id: String? = null,
        val count: Int? = null,
    )

    private data class IssueComments_IssueQuery(
        val commentsCount: Int? = null,
    )
}
