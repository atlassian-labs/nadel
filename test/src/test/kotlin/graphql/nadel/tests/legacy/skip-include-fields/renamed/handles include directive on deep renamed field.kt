package graphql.nadel.tests.legacy.`skip-include-fields`.renamed

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on deep renamed field` : NadelLegacyIntegrationTest(
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
                  name: ID @renamed(from: "details.id")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  details: FooDetails
                }
                type FooDetails {
                  id: ID
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
        val details: Service_FooDetails? = null,
    )

    private data class Service_FooDetails(
        val id: String? = null,
    )
}
