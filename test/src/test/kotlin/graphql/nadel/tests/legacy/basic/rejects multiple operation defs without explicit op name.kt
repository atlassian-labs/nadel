package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `rejects multiple operation defs without explicit op name` : NadelLegacyIntegrationTest(
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
            },
        ),
    ),
)
