package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `query with three nested hydrations and synthetic fields` : NadelLegacyIntegrationTest(
    query = """
        query {
          foos {
            bar {
              name
              nestedBar {
                name
                nestedBar {
                  name
                }
              }
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
                  barsQuery: BarQuery
                }
                type BarQuery {
                  bar: Bar
                  barsById(id: [ID]): [Bar]
                }
                type Bar {
                  barId: ID
                  name: String
                  nestedBar: Bar
                  @hydrated(
                    service: "Bar"
                    field: "barsQuery.barsById"
                    arguments: [{ name: "id" value: "${'$'}source.nestedBarId"}]
                    identifiedBy: "barId"
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  barId: ID
                  name: String
                  nestedBarId: ID
                }
                type BarQuery {
                  bar: Bar
                  barsById(id: [ID]): [Bar]
                }
                type Query {
                  barsQuery: BarQuery
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barsQuery") {
                        Unit
                    }
                }
                wiring.type("BarQuery") { type ->
                    val barsById = listOf(
                        Bar_Bar(
                            barId = "bar3",
                            name = "Bar 3",
                            nestedBarId = null,
                        ),
                        Bar_Bar(
                            barId = "bar1",
                            name = "Bar 1",
                            nestedBarId = "nestedBar1",
                        ),
                        Bar_Bar(
                            barId = "bar2",
                            name = "Bar 2",
                            nestedBarId = "nestedBar2",
                        ),
                        Bar_Bar(
                            barId = "nestedBar1",
                            name = "NestedBarName1",
                            nestedBarId = "nestedBarId456",
                        ),
                        Bar_Bar(
                            barId = "nestedBarId456",
                            name = "NestedBarName2",
                        ),
                    ).associateBy { it.barId }

                    type.dataFetcher("barsById") { env ->
                        env.getArgument<List<String>>("id")?.mapNotNull(barsById::get)
                    }
                }
            },
        ),
        Service(
            name = "Foo",
            overallSchema = """
                type Query {
                  foos: [Foo]
                }
                type Foo {
                  name: String
                  bar: Bar
                  @hydrated(
                    service: "Bar"
                    field: "barsQuery.barsById"
                    arguments: [{ name: "id" value: "${'$'}source.barId"}]
                    identifiedBy: "barId"
                    batchSize: 2
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: ID
                  name: String
                }
                type Query {
                  foos: [Foo]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foos") { env ->
                        listOf(Foo_Foo(barId = "bar1"), Foo_Foo(barId = "bar2"), Foo_Foo(barId = "bar3"))
                    }
                }
            },
        ),
    ),
) {
    private data class Bar_Bar(
        val barId: String? = null,
        val name: String? = null,
        val nestedBarId: String? = null,
    )

    private data class Bar_BarQuery(
        val bar: Bar_Bar? = null,
        val barsById: List<Bar_Bar?>? = null,
    )

    private data class Foo_Foo(
        val barId: String? = null,
        val name: String? = null,
    )
}
