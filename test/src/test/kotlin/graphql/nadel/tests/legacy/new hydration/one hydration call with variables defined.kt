package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `one hydration call with variables defined` : NadelLegacyIntegrationTest(
    query = """
        query(${'$'}var: ID) {
          foo(id: ${'$'}var) {
            bar {
              id
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service2",
            overallSchema = """
                type Query {
                  barById(id: ID): Bar
                }
                type Bar {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: ID
                  name: String
                }
                type Query {
                  barById(id: ID): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("id") == "barId") {
                            Service2_Bar(id = "barId", name = "Bar1")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "service1",
            overallSchema = """
                type Query {
                  foo(id: ID): Foo
                }
                type Foo {
                  id: ID
                  bar: Bar
                  @hydrated(
                    field: "barById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
                  )
                  barLongerInput: Bar
                  @hydrated(
                    field: "barById"
                    arguments: [{name: "id" value: "${'$'}source.fooDetails.externalBarId"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: ID
                  fooDetails: FooDetails
                  id: ID
                }
                type FooDetails {
                  externalBarId: ID
                }
                type Query {
                  foo(id: ID): Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(barId = "barId")
                    }
                }
            },
        ),
    ),
) {
    private data class Service2_Bar(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Service1_Foo(
        val barId: String? = null,
        val fooDetails: Service1_FooDetails? = null,
        val id: String? = null,
    )

    private data class Service1_FooDetails(
        val externalBarId: String? = null,
    )
}
