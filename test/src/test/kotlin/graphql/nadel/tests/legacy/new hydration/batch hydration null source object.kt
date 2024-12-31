package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batch hydration null source object` : NadelLegacyIntegrationTest(
    query = """
        query {
          myIssues {
            title
            assignee {
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  myIssues(n: Int! = 10): [Issue]
                }
                type Issue {
                  title: String
                  assigneeId: ID
                  assignee: User
                  @hydrated(
                    service: "users"
                    field: "usersByIds"
                    arguments: [{name: "ids" value: "${'$'}source.assigneeId"}]
                    inputIdentifiedBy: [{sourceId: "assigneeId", resultId: "id"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  topIssue: Issue
                  myIssues(n: Int! = 10): [Issue]
                }
                type Issue {
                  title: String
                  assigneeId: ID
                  collaboratorIds: [ID!]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("myIssues") { env ->
                        if (env.getArgument<Any?>("n") == 10) {
                            listOf(Issues_Issue(assigneeId = "user-256", title = "Popular"), null)
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "users",
            overallSchema = """
                type Query {
                  usersByIds(ids: [ID!]!): [User]
                }
                type User {
                  id: ID!
                  name: String
                  email: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  usersByIds(ids: [ID!]!): [User]
                }
                type User {
                  id: ID!
                  name: String
                  email: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("usersByIds") { env ->
                        if (env.getArgument<Any?>("ids") == listOf("user-256")) {
                            listOf(Users_User(id = "user-256", name = "2^8"))
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
        val title: String? = null,
        val assigneeId: String? = null,
        val collaboratorIds: List<String>? = null,
    )

    private data class Users_User(
        val id: String? = null,
        val name: String? = null,
        val email: String? = null,
    )
}
