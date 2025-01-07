package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename inside deep rename` : NadelLegacyIntegrationTest(
    query = """
        query {
          issue {
            extras {
              ownerName
            }
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
                  extras: IssueExtra @renamed(from: "details.extras")
                }
                type IssueExtra {
                  ownerName: String @renamed(from: "owner.name")
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
                  extras: IssueExtra
                }
                type IssueExtra {
                  owner: User
                }
                type User {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("first") { env ->
                        Issues_Issue(
                            details = Issues_IssueDetail(
                                extras = Issues_IssueExtra(
                                    owner = Issues_User(name = "Franklin"),
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
        val extras: Issues_IssueExtra? = null,
    )

    private data class Issues_IssueExtra(
        val owner: Issues_User? = null,
    )

    private data class Issues_User(
        val name: String? = null,
    )
}
