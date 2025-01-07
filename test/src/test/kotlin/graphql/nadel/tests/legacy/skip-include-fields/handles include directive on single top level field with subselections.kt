package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on single top level field with subselections` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo @include(if: ${'$'}test) {
            id
          }
        }
    """.trimIndent(),
    variables = mapOf("test" to false),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  id: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  id: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class Service_Foo(
        val id: String? = null,
    )
}
