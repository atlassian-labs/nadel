package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename inside rename` : NadelLegacyIntegrationTest(
    query = """
        query {
          first: issue {
            __typename
            name
          }
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
                  issue: JiraIssue @renamed(from: "first")
                }
                type JiraIssue @renamed(from: "Issue") {
                  name: String @renamed(from: "details.name")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  first: Issue
                }
                type Issue {
                  details: IssueDetail
                }
                type IssueDetail {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("first") { env ->
                        if (env.field.resultKey == "rename__issue__first") {
                            Issues_Issue(details = Issues_IssueDetail(name = "name-from-details-2"))
                        } else if (env.field.resultKey == "rename__first__first") {
                            Issues_Issue(details = Issues_IssueDetail(name = "name-from-details"))
                        } else {
                            null
                        }
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
        val name: String? = null,
    )
}
