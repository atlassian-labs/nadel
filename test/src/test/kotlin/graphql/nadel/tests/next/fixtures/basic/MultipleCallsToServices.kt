package graphql.nadel.tests.next.fixtures.basic

import graphql.nadel.tests.next.NadelIntegrationTest

class MultipleCallsToServices : NadelIntegrationTest(
    query = """
        query {
          echo
          whereAmI
          nearestOffice
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "hello",
            overallSchema = """
                type Query {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") {
                        it
                            .dataFetcher("echo") {
                                "Hello World"
                            }
                    }
            },
        ),
        Service(
            name = "location",
            overallSchema = """
                type Query {
                  whereAmI: String
                  nearestOffice: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") {
                        it
                            .dataFetcher("whereAmI") {
                                "Earth"
                            }
                            .dataFetcher("nearestOffice") {
                                "Sydney"
                            }
                    }
            },
        ),
    ),
)
