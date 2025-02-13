package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `rename with first last path element returning null` : NadelLegacyIntegrationTest(
    query = """
        query {
          issue {
            name
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  name: String @renamed(from: "details.name")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  details: IssueDetails
                }
                type IssueDetails {
                  name: String
                }
                type Query {
                  issue: Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        Issues_Issue(details = Issues_IssueDetails(name = null))
                    }
                }
            },
        ),
    ),
) {
    private data class Issues_Issue(
        val details: Issues_IssueDetails? = null,
    )

    private data class Issues_IssueDetails(
        val name: String? = null,
    )
}
