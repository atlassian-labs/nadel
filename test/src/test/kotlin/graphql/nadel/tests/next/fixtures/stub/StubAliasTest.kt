package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest

class StubAliasTest : NadelIntegrationTest(
    query = """
        {
          issue {
            id: key
            description
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
                            .dataFetcher("issue") { env ->
                                Issue(
                                    id = "123",
                                    title = "Wow an issue",
                                    description = "Stop procrastinating and do the work",
                                )
                            }
                    }
            },
        ),
    ),
)
