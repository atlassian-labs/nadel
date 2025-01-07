package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `typename is resolved on namespaced fields` : NadelLegacyIntegrationTest(
    query = """
        {
          issue {
            __typename
            aliasTypename: __typename
            getIssue {
              __typename
              aliasTypename: __typename
              text
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
    ),
) {
    private data class Issues_Issue(
        val id: String? = null,
        val text: String? = null,
    )

    private data class Issues_IssueQuery(
        val getIssue: Issues_Issue? = null,
    )
}
