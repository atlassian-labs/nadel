package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed type inside batch hydration` : NadelLegacyIntegrationTest(
    query = """
        query {
          users {
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
                  issuesByIds(id: [ID!]): [Issue]
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
                  issuesByIds(id: [ID!]): [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val issuesByIds = listOf(
                        IssueService_Issue(
                            details = IssueService_Details(name = "Details of issue one"),
                            id = "issue-1",
                        ),
                        IssueService_Issue(
                            details =
                            IssueService_Details(
                                name =
                                "Issue two",
                            ),
                            id = "issue-2",
                        ),
                        IssueService_Issue(
                            details =
                            IssueService_Details(name = "Issue four – no wait three"),
                            id = "issue-3",
                        ),
                    ).associateBy { it.id }

                    type.dataFetcher("issuesByIds") { env ->
                        env.getArgument<List<String>>("id")?.map(issuesByIds::get)
                    }
                }
            },
        ),
        Service(
            name = "UserService",
            overallSchema = """
                type Query {
                  users: [User]
                }
                type User {
                  issueId: ID
                  issue: Issue @hydrated(
                    service: "IssueService"
                    field: "issuesByIds"
                    arguments: [
                      {name: "id" value: "${'$'}source.issueId"}
                    ]
                    identifiedBy: "id"
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  users: [User]
                }
                type User {
                  issueId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("users") { env ->
                        listOf(
                            UserService_User(issueId = "issue-1"),
                            UserService_User(issueId = "issue-2"),
                            UserService_User(issueId = "issue-3"),
                        )
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
