package graphql.nadel.tests.next.fixtures.introspection

import graphql.nadel.tests.next.NadelIntegrationTest

class NamespaceLikeShortCircuitsIntrospectionTest : NadelIntegrationTest(
    query = """
        {
          test {
            __typename
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  test: TestQuery
                }
                type TestQuery {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("echo") { env ->
                                "echo"
                            }
                    }
            },
        ),
    ),
)
