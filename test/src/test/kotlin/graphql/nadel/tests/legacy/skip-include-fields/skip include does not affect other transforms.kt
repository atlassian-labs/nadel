package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `skip include does not affect other transforms` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo {
            foo {
              id @skip(if: ${'$'}test)
            }
            bar: foo @include(if: ${'$'}test) {
              id
            }
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
                  foo: Foo
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  foo: Foo
                  id: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service_Foo(foo = Service_Foo(id = "FOO-1"))
                    }
                }
            },
        ),
    ),
) {
    private data class Service_Foo(
        val foo: Service_Foo? = null,
        val id: String? = null,
    )
}
