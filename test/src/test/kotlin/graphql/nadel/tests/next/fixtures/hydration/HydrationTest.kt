package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest

class HydrationTest : NadelIntegrationTest(
    query = """
        query {
          issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
            id
            key
            assignee {
              id
              name
            }
          }
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                  key: String
                  assigneeId: ID @hidden
                  assignee: User
                    @hydrated(
                      field: "userById"
                      arguments: [{name: "id", value: "$source.assigneeId"}]
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val key: String,
                    val assigneeId: String? = null,
                )

                val issuesById = listOf(
                    Issue(
                        id = "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                        key = "GQLGW-1",
                        assigneeId = "ari:cloud:identity::user/1",
                    )
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issueById") {
                            issuesById[it.getArgument("id")]
                        }
                    }
            },
        ),
        Service(
            name = "identity",
            overallSchema = """
                type Query {
                  userById(id: ID!): User
                }
                type User {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val name: String,
                )

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                        name = "Franklin Wang",
                    ),
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("userById") {
                            usersById[it.getArgument("id")]
                        }
                    }
            },
        ),
    ),
)
