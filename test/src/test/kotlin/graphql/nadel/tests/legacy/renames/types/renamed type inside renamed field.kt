package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed type inside renamed field` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            __typename
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "IssueService",
            overallSchema = """
                type Query {
                  issues: [JiraIssue] @renamed(from: "all")
                }
                type JiraIssue @renamed(from: "Issue") {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  all: [Issue]
                }
                type Issue {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("all") { env ->
                        listOf(IssueService_Issue(), null, IssueService_Issue(), IssueService_Issue())
                    }
                }
            },
        ),
    ),
) {
    private data class IssueService_Issue(
        val id: String? = null,
    )
}
