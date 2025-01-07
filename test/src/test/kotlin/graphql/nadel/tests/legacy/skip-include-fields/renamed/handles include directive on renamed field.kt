package graphql.nadel.tests.legacy.`skip-include-fields`.renamed

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on renamed field` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo {
            name @include(if: ${'$'}test)
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
                  name: String @renamed(from: "id")
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
