package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `can execute document with multiple operation definitions` : NadelLegacyIntegrationTest(
    operationName = "Test",
    query = """
        query Foo {
          foo
        }
        query Test {
          test: foo
        }
        query Dog {
          dog: foo
        }
        query Meow {
          cat: foo
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
