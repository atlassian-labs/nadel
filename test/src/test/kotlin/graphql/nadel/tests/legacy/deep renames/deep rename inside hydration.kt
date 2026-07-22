package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename inside hydration` : NadelLegacyIntegrationTest(
    query = """
        query {
          me {
            issue {
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "IssueService",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue
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
                  issueById(id: ID!): Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issueById") { env ->
                        if (env.getArgument<Any?>("id") == "issue-1") {
                            IssueService_Issue(detail = IssueService_IssueDetails(detailName = "Detail-1"))
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "UserService",
            overallSchema = """
                type Query {
                  me: User
                }
                type User {
                  issueId: ID
                  issue: Issue @hydrated(
                    field: "issueById"
                    arguments: [
                      {name: "id" value: "${'$'}source.issueId"}
                    ]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  me: User
                }
                type User {
                  issueId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("me") { env ->
                        UserService_User(issueId = "issue-1")
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

    private data class UserService_User(
        val issueId: String? = null,
    )
}
