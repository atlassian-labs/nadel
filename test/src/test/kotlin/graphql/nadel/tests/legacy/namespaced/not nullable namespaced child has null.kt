package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `not nullable namespaced child has null` : NadelLegacyIntegrationTest(
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
                        Issues_IssueQuery(getIssue = Issues_Issue(text = "Foo"))
                    }
                }
            },
        ),
        Service(
            name = "IssueSearch",
            overallSchema = """
                extend type IssueQuery {
                  search: SearchResult!
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
                  search: SearchResult!
                }
                type SearchResult {
                  id: ID
                  count: Int
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        IssueSearch_IssueQuery(search = null)
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

    private data class IssueSearch_IssueQuery(
        val search: IssueSearch_SearchResult? = null,
    )

    private data class IssueSearch_SearchResult(
        val id: String? = null,
        val count: Int? = null,
    )
}
