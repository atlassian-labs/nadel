package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed type inside deep rename` : NadelLegacyIntegrationTest(
    query = """
        query {
          issueById(id: "issue-1") {
            assignee {
              name
              __typename
              friends {
                __typename
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
                  assignee: IssueUser @renamed(from: "details.assignee")
                }
                type IssueUser @renamed(from: "User") {
                  name: String
                  friends: [IssueUser]
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
                  assignee: User
                }
                type User {
                  name: String
                  friends: [User]
                }
                type Query {
                  issueById(id: ID!): Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issueById") { env ->
                        if (env.getArgument<Any?>("id") == "issue-1") {
                            IssueService_Issue(
                                details =
                                IssueService_Details(
                                    assignee =
                                    IssueService_User(
                                        friends =
                                        listOf(IssueService_User(), IssueService_User()),
                                        name = "Franklin",
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
    private data class IssueService_Details(
        val name: String? = null,
        val assignee: IssueService_User? = null,
    )

    private data class IssueService_Issue(
        val id: String? = null,
        val details: IssueService_Details? = null,
    )

    private data class IssueService_User(
        val name: String? = null,
        val friends: List<IssueService_User?>? = null,
    )
}
