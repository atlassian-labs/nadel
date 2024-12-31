package graphql.nadel.tests.legacy.`new hydration`

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `synthetic hydration list with batching forwards error` : NadelLegacyIntegrationTest(
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
                  barsQuery: BarsQuery
                }
                type BarsQuery {
                  barsById(id: [ID]): [Bar]
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
                type BarsQuery {
                  barsById(id: [ID]): [Bar]
                }
                type Query {
                  barsQuery: BarsQuery
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barsQuery") {
                        Unit
                    }
                }
                wiring.type("BarsQuery") { type ->
                    type.dataFetcher("barsById") { env ->
                        if (env.getArgument<Any?>("id") == listOf("barId1", "barId2", "barId3")) {
                            DataFetcherResult
                                .newResult<Any>()
                                .data(null)
                                .errors(
                                    listOf(
                                        toGraphQLError(
                                            mapOf(
                                                "message"
                                                    to "Some error occurred",
                                            ),
                                        ),
                                    ),
                                ).build()
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
                  bar: [Bar]
                  @hydrated(
                    service: "service2"
                    field: "barsQuery.barsById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: [ID]
                  id: ID
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(barId = listOf("barId1", "barId2", "barId3"))
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

    private data class Service2_BarsQuery(
        val barsById: List<Service2_Bar?>? = null,
    )

    private data class Service1_Foo(
        val barId: List<String?>? = null,
        val id: String? = null,
    )
}
