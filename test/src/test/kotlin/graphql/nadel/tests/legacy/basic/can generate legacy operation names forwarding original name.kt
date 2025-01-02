package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `can generate legacy operation names forwarding original name` : NadelLegacyIntegrationTest(
    query = """
        query Original {
          foo
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "test",
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
