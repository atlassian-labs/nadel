package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles skip directive on single field whose parent returns object` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo {
            id @skip(if: ${'$'}test)
          }
        }
    """.trimIndent(),
    variables = mapOf("test" to true),
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
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service_Foo()
                    }
                }
            },
        ),
    ),
) {
    private data class Service_Foo(
        val id: String? = null,
    )
}
