package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles skip include directive on field with subselections` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!, ${'$'}invertTest: Boolean! = false, ${'$'}other: Boolean! = true) {
          foo {
            foo @skip(if: ${'$'}test) {
              __typename @skip(if: ${'$'}invertTest)
              id @include(if: ${'$'}test)
            }
            bar: foo @include(if: ${'$'}other) {
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
