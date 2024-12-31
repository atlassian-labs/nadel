package graphql.nadel.tests.legacy.`skip-include-fields`.hydration

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive field with hydrated parent` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foo {
            water {
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
                  foo: Foo @renamed(from: "bar")
                  fooById(id: ID): Foo
                }
                type Foo {
                  id: String
                  water: Foo @hydrated(
                    service: "service"
                    field: "fooById"
                    arguments: [
                      {name: "id" value: "${'$'}source.id"}
                    ]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  bar: Foo
                  fooById(id: ID): Foo
                }
                type Foo {
                  id: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("bar") { env ->
                            Service_Foo(id = "Foo-1")
                        }.dataFetcher("fooById") { env ->
                            if (env.getArgument<Any?>("id") == "Foo-1") {
                                Service_Foo()
                            } else {
                                null
                            }
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
