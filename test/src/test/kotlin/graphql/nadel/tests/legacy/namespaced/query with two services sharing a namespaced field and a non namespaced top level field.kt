package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `query with two services sharing a namespaced field and a non namespaced top level field` : NadelLegacyIntegrationTest(
    query = """
        {
          issue {
            getIssue {
              text
            }
            search {
              count
            }
          }
          conf {
            title
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
                  conf: Page
                }
                type IssueQuery {
                  getIssue: Issue
                }
                type Issue {
                  id: ID
                  text: String
                }
                type Page {
                  id: ID
                  title: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue: IssueQuery
                  conf: Page
                }
                type Page {
                  id: ID
                  title: String
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
                    type
                        .dataFetcher("conf") { env ->
                            Issues_Page(title = "Page title")
                        }
                        .dataFetcher("issue") { env ->
                            Issues_IssueQuery(getIssue = Issues_Issue(text = "Foo"))
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
                        IssueSearch_IssueQuery(search = IssueSearch_SearchResult(count = 100))
                    }
                }
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

    private data class Issues_Page(
        val id: String? = null,
        val title: String? = null,
    )

    private data class IssueSearch_IssueQuery(
        val search: IssueSearch_SearchResult? = null,
    )

    private data class IssueSearch_SearchResult(
        val id: String? = null,
        val count: Int? = null,
    )
}
