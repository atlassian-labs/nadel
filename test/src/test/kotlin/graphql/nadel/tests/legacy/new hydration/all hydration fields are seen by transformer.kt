package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `all hydration fields are seen by transformer` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            bar {
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
                  bars: BarQuery
                }
                type BarQuery {
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
                  bars: BarQuery
                }
                type BarQuery {
                  barById(id: ID): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("bars") {
                        Unit
                    }
                }
                wiring.type("BarQuery") { type ->
                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("id") == "barId") {
                            Service2_Bar(name = "Bar1")
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
                  foo: Foo
                }
                type Foo {
                  id: ID
                  bar: Bar
                  @hydrated(
                    service: "service2"
                    field: "bars.barById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
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
                  foo: Foo
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

    private data class Service2_BarQuery(
        val barById: Service2_Bar? = null,
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
