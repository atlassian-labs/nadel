package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename inside renamed type` : NadelLegacyIntegrationTest(
    query = """
        query {
          first {
            __typename
            name
          }
          second: first {
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
                  first: JiraIssue
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
                        if (env.field.resultKey == "first") {
                            Issues_Issue(details = Issues_IssueDetail(name = "name-from-details"))
                        } else if (env.field.resultKey == "second") {
                            Issues_Issue(details = Issues_IssueDetail(name = "name-from-details-2"))
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
