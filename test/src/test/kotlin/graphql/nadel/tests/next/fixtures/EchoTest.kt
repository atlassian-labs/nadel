package graphql.nadel.tests.next.fixtures

import graphql.nadel.tests.next.NadelIntegrationTest

class EchoTest : NadelIntegrationTest(
    query = """
        query {
          echo
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "",
            overallSchema = """
                type Query {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") {
                        it.dataFetcher("echo") {
                            "Hello World"
                        }
                    }
            },
        ),
    ),
)
