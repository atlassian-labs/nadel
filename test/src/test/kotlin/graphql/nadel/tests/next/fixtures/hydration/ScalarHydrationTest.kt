package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest

class ScalarHydrationTest : NadelIntegrationTest(
    query = """
        query {
          issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
            id
            key
            linkedCount
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
                  linkedCount(id: ID!): Int
                }
                type Issue {
                  id: ID!
                  key: String
                  linkedCount: Int @hydrated(field: "linkedCount", arguments: [{name: "id", value: "$source.id"}])
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
                        type
                            .dataFetcher("issueById") {
                                issuesById[it.getArgument("id")]
                            }
                            .dataFetcher("linkedCount") {
                                100
                            }
                    }
            },
        ),
    ),
)
