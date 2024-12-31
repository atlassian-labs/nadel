package graphql.nadel.tests.legacy.`skip-include-fields`.renamed

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on field with deep renamed parent` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo {
            bar {
              id @include(if: ${'$'}test)
            }
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
                  bar: Bar @renamed(from: "details.bar")
                }
                type Bar {
                  id: ID
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
                  bar: Bar
                }
                type Bar {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service_Foo(details = Service_FooDetails(bar = Service_Bar()))
                    }
                }
            },
        ),
    ),
) {
    private data class Service_Bar(
        val id: String? = null,
    )

    private data class Service_Foo(
        val details: Service_FooDetails? = null,
    )

    private data class Service_FooDetails(
        val bar: Service_Bar? = null,
    )
}
