package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `rename inside multiple hydrations` : NadelLegacyIntegrationTest(
    query = """
        query {
          foos {
            bar {
              title
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
                  title: String @renamed(from: "name")
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
                            Service2_Bar(name = "Bar1")
                        } else if (env.getArgument<Any?>("id") == "barId2") {
                            Service2_Bar(name = "Bar2")
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
                  foos: [Foo]
                }
                type Foo {
                  id: ID
                  bar: Bar
                  @hydrated(
                    service: "service2"
                    field: "barById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
                  )
                  barLongerInput: Bar
                  @hydrated(
                    service: "service2"
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
                  foos: [Foo]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foos") { env ->
                        listOf(Service1_Foo(barId = "barId"), Service1_Foo(barId = "barId2"))
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
