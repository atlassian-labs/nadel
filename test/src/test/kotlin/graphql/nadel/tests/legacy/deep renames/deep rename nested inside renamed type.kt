package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename nested inside renamed type` : NadelLegacyIntegrationTest(
    query = """
        query {
          first {
            user {
              name
            }
          }
          second: first {
            __typename
            user {
              name
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
                  first: JiraIssue
                }
                type JiraIssue @renamed(from: "Issue") {
                  user: User
                }
                type User {
                  name: String @renamed(from: "details.firstName")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  first: Issue
                }
                type Issue {
                  user: User
                }
                type UserDetails {
                  firstName: String
                }
                type User {
                  id: ID
                  details: UserDetails
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("first") { env ->
                        if (env.field.resultKey == "first") {
                            Issues_Issue(
                                user =
                                Issues_User(
                                    details =
                                    Issues_UserDetails(
                                        firstName =
                                        "name-from-details",
                                    ),
                                ),
                            )
                        } else if (env.field.resultKey == "second") {
                            Issues_Issue(
                                user =
                                Issues_User(
                                    details =
                                    Issues_UserDetails(
                                        firstName =
                                        "name-from-details-2",
                                    ),
                                ),
                            )
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
        val user: Issues_User? = null,
    )

    private data class Issues_User(
        val id: String? = null,
        val details: Issues_UserDetails? = null,
    )

    private data class Issues_UserDetails(
        val firstName: String? = null,
    )
}
