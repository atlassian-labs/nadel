package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed type inside hydration that returns null` : NadelLegacyIntegrationTest(
    query = """
        query {
          me {
            issue {
              details {
                __typename
                name
              }
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
                  details: IssueDetails
                }
                type IssueDetails @renamed(from: "Details") {
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  id: ID
                  details: Details
                }
                type Details {
                  name: String
                }
                type Query {
                  issueById(id: ID!): Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issueById") { env ->
                        if (env.getArgument<Any?>("id") == "issue-2") {
                            null
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
                    service: "IssueService"
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
                        UserService_User(issueId = "issue-2")
                    }
                }
            },
        ),
    ),
) {
    private data class IssueService_Details(
        val name: String? = null,
    )

    private data class IssueService_Issue(
        val id: String? = null,
        val details: IssueService_Details? = null,
    )

    private data class UserService_User(
        val issueId: String? = null,
    )
}
