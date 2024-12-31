package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `typename is sent owning service even when namespaced field and type are defined in different services` :
    NadelLegacyIntegrationTest(
        query = """
            {
              issue {
                __typename
                getIssue {
                  text
                }
              }
            }
        """.trimIndent(),
        variables = emptyMap(),
        services =
        listOf(
            Service(
                name = "Issues",
                overallSchema = """
                    directive @namespaced on FIELD_DEFINITION
                    type IssueQuery {
                      getIssue: Issue
                    }
                    extend type IssueQuery {
                      getIssues: [Issue]
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
                    extend type IssueQuery {
                      getIssues: [Issue]
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
                    type Query {
                      issue: IssueQuery @namespaced
                    }
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
        val getIssues: List<Issues_Issue?>? = null,
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
