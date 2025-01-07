package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `can execute single named operation when operation name is empty` : NadelLegacyIntegrationTest(
    query = """
        query Test {
          test: foo
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
                        "Test Working"
                    }
                }
            },
        ),
    ),
)
