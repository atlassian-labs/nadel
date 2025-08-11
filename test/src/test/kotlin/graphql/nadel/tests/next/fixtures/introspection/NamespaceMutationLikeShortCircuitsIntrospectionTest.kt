package graphql.nadel.tests.next.fixtures.introspection

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

class NamespaceMutationLikeShortCircuitsIntrospectionTest : NadelIntegrationTest(
    query = """
        mutation {
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
                type Mutation {
                  test: TestMutation
                }
                type TestMutation {
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
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .shortCircuitEmptyQuery { true }
    }
}
