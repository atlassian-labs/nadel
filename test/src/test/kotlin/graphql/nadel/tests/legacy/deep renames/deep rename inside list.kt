package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename inside list` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
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
                  issues: [JiraIssue] @renamed(from: "all")
                }
                type JiraIssue @renamed(from: "Issue") {
                  name: String @renamed(from: "details.key")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  all: [Issue]
                }
                type Issue {
                  details: IssueDetail
                }
                type IssueDetail {
                  key: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("all") { env ->
                        listOf(
                            Issues_Issue(details = Issues_IssueDetail(key = "GQLGW-1012")),
                            Issues_Issue(details = null),
                            Issues_Issue(
                                details =
                                Issues_IssueDetail(
                                    key =
                                    "Fix the bug",
                                ),
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Issues_Issue(
        val details: Issues_IssueDetail? = null,
    )

    private data class Issues_IssueDetail(
        val key: String? = null,
    )
}
