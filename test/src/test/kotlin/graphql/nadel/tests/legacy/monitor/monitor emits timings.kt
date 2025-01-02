package graphql.nadel.tests.legacy.monitor

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `monitor emits timings` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foo: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        null
                    }
                }
            },
        ),
    ),
)
