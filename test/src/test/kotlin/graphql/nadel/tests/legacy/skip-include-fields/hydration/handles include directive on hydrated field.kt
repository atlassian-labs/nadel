package graphql.nadel.tests.legacy.`skip-include-fields`.hydration

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on hydrated field` : NadelLegacyIntegrationTest(
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
                  test(id: ID): String
                }
                type Foo {
                  name: String @hydrated(
                    field: "test"
                    arguments: [
                      {name: "id" value: "${'$'}source.id"}
                    ]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                  test(id: ID): String
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
