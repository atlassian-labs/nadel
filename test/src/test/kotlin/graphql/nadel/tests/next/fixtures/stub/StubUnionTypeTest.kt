package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest
import java.lang.UnsupportedOperationException

class StubUnionTypeTest : NadelIntegrationTest(
    query = """
        {
          thing {
            __typename
            ... on Issue {
              title
            }
            ... on WorkItem {
              __typename
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  thing: Thing
                }
                type Issue {
                  id: ID!
                  title: String
                  description: String
                }
                type WorkItem {
                  description: String
                }
                union Thing @stubbed = Issue | WorkItem
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
                type Issue {
                  id: ID!
                  title: String
                  description: String
                }
                type WorkItem {
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
                            .dataFetcher("thing") { env ->
                                throw UnsupportedOperationException()
                            }
                    }
            },
        ),
    ),
)
