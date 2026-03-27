package graphql.nadel.tests.legacy.`new hydration`

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `query with hydration that fail with errors are reflected in the result` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
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
                  bar: Bar
                  barById(id: ID): Bar
                }
                type Bar {
                  name: String
                  nestedBar: Bar
                  @hydrated(
                    field: "barById"
                    arguments: [{name: "id" value: "${'$'}source.nestedBarId"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: ID
                  name: String
                  nestedBarId: ID
                }
                type Query {
                  bar: Bar
                  barById(id: ID): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("id") == "barId123") {
                            DataFetcherResult
                                .newResult<Any>()
                                .data(null)
                                .errors(
                                    listOf(
                                        toGraphQLError(
                                            mapOf(
                                                "message" to "Error during hydration",
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
            name = "Foo",
            overallSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  name: String
                  bar: Bar
                  @hydrated(
                    field: "barById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: ID
                  name: String
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Foo_Foo(barId = "barId123")
                    }
                }
            },
        ),
    ),
) {
    private data class Bar_Bar(
        val id: String? = null,
        val name: String? = null,
        val nestedBarId: String? = null,
    )

    private data class Foo_Foo(
        val barId: String? = null,
        val name: String? = null,
    )
}
