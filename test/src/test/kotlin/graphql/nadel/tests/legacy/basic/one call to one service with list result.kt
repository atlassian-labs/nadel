package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `one call to one service with list result` : NadelLegacyIntegrationTest(
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
                  foo: [String]
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: [String]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        listOf("foo1", "foo2")
                    }
                }
            },
        ),
    ),
)
