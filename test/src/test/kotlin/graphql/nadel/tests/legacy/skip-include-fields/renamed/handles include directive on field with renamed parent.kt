package graphql.nadel.tests.legacy.`skip-include-fields`.renamed

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on field with renamed parent` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo {
            id @include(if: ${'$'}test)
          }
        }
    """.trimIndent(),
    variables = mapOf("test" to false),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foo: Foo @renamed(from: "bar")
                }
                type Foo {
                  id: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  bar: Foo
                }
                type Foo {
                  id: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("bar") { env ->
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
