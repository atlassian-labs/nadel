package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `synthetic hydration with renamed type` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            id
            fooBar {
              id
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Bar",
            overallSchema = """
                type Query {
                  bars: BarQuery
                }
                type BarQuery {
                  barById(id: ID!): Bar
                }
                type Bar {
                  id: ID!
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: ID!
                }
                type BarQuery {
                  barById(id: ID!): Bar
                }
                type Query {
                  bars: BarQuery
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
                        if (env.getArgument<Any?>("id") == "hydrated-bar") {
                            Bar_Bar(id = "hydrated-bar")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "Foo",
            overallSchema = """
                type Query {
                  foo: Foo @renamed(from: "fooOriginal")
                }
                type Foo {
                  id: ID!
                  fooBar: Bar
                  @hydrated(
                    service: "Bar"
                    field: "bars.barById"
                    arguments: [{name: "id" value: "${'$'}source.fooBarId"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  fooBarId: ID
                  id: ID!
                }
                type Query {
                  fooOriginal: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("fooOriginal") { env ->
                        Foo_Foo(fooBarId = "hydrated-bar", id = "Foo")
                    }
                }
            },
        ),
    ),
) {
    private data class Bar_Bar(
        val id: String? = null,
    )

    private data class Bar_BarQuery(
        val barById: Bar_Bar? = null,
    )

    private data class Foo_Foo(
        val fooBarId: String? = null,
        val id: String? = null,
    )
}
