package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration list input` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
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
                        if (env.getArgument<Any?>("id") == "barId1") {
                            Service2_Bar(id = "barId1", name = "Bar1")
                        } else if (env.getArgument<Any?>("id") == "barId3") {
                            Service2_Bar(id = "barId3", name = "Bar4")
                        } else if (env.getArgument<Any?>("id") == "barId2") {
                            Service2_Bar(id = "barId2", name = "Bar3")
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
                    field: "barById"
                    arguments: [{name: "id" value: "${'$'}source.barIds"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barIds: [ID]
                  id: ID
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(barIds = listOf("barId1", "barId2", "barId3"))
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
        val barIds: List<String?>? = null,
        val id: String? = null,
    )
}
