package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename inside batch hydration null object` : NadelLegacyIntegrationTest(
    query = """
        query {
          users {
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
                  issuesByIds(id: [ID!]): [Issue]
                }
                type Issue {
                  name: String @renamed(from: "detail.detailName")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  id: ID
                  detail: IssueDetails
                }
                type IssueDetails {
                  detailName: String
                }
                type Query {
                  issuesByIds(id: [ID!]): [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val issuesByIds = listOf(
                        IssueService_Issue(
                            detail = IssueService_IssueDetails(
                                detailName = "Memes are the DNA of the soul",
                            ),
                            id = "issue-1",
                        ),
                        IssueService_Issue(
                            detail = IssueService_IssueDetails(
                                detailName = "Names are arbitrary",
                            ),
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
    private data class IssueService_Issue(
        val id: String? = null,
        val detail: IssueService_IssueDetails? = null,
    )

    private data class IssueService_IssueDetails(
        val detailName: String? = null,
    )

    private data class UserService_User(
        val issueId: String? = null,
    )
}
