package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest

class StubNestedTest : NadelIntegrationTest(
    query = """
        {
          issue {
            id
            key
            description
            related {
              key
              user {
                __typename
                name
              }
              related {
                key
                id: key
                related {
                  user {
                    name
                  }
                }
              }
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  id: ID!
                  key: String @stubbed
                  title: String
                  description: String
                  related: [Issue]
                  user: User
                }
                type User @stubbed {
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  id: ID!
                  title: String
                  description: String
                  related: [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val title: String,
                    val description: String,
                    val related: List<Issue>,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issue") { env ->
                                val root = Issue(
                                    id = "123",
                                    title = "Wow an issue",
                                    description = "Stop procrastinating and do the work",
                                    related = emptyList(),
                                )

                                root.copy(
                                    related = listOf(
                                        root.copy(
                                            related = listOf(root),
                                        ),
                                        Issue(
                                            id = "456",
                                            title = "Wow a work item?",
                                            description = "Still, stop procrastinating and do the work",
                                            related = listOf(
                                                root.copy(
                                                    related = listOf(root),
                                                ),
                                            ),
                                        ),
                                    )
                                )
                            }
                    }
            },
        ),
    ),
)
