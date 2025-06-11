package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest

class StubListTest : NadelIntegrationTest(
    query = """
        {
          issues {
            id
            key
            description
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID!
                  key: String @stubbed
                  title: String
                  description: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID!
                  title: String
                  description: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val title: String,
                    val description: String,
                )
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") { env ->
                                listOf(
                                    Issue(
                                        id = "123",
                                        title = "Wow an issue",
                                        description = "Stop procrastinating and do the work",
                                    ),
                                    null,
                                    Issue(
                                        id = "456",
                                        title = "Wow another issue",
                                        description = "Wow they're piling up",
                                    ),
                                )
                            }
                    }
            },
        ),
    ),
)
