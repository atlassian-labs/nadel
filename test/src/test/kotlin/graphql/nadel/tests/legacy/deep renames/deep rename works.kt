package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename works` : NadelLegacyIntegrationTest(
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
            name = "IssueService",
            overallSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  name: String @renamed(from: "detail.detailName")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  detail: IssueDetails
                }
                type IssueDetails {
                  detailName: String
                }
                type Query {
                  issue: Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        IssueService_Issue(detail = IssueService_IssueDetails(detailName = "My Issue"))
                    }
                }
            },
        ),
    ),
) {
    private data class IssueService_Issue(
        val detail: IssueService_IssueDetails? = null,
    )

    private data class IssueService_IssueDetails(
        val detailName: String? = null,
    )
}
