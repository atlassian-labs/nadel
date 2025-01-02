package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on single top level field` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo @include(if: ${'$'}test)
        }
    """.trimIndent(),
    variables = mapOf("test" to false),
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
