package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `basic hydration with default array argument values` : NadelLegacyIntegrationTest(
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
                  barById(id: ID, test: [String]): Bar
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
                  barById(id: ID, test: [String]): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val barById = listOf(
                        Service2_Bar(id = "barId", name = "Bar1"),
                    ).associateBy { it.id }

                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("test") == listOf("Hello", "World")) {
                            barById[env.getArgument<String>("id")]
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
                  bar(test: [String] = ["Hello", "World"]): Bar @hydrated(
                    service: "service2"
                    field: "barById"
                    arguments: [
                      {name: "id" value: "${'$'}source.barId"}
                      {name: "test" value: "${'$'}argument.test"}
                    ]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: ID
                  id: ID
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

    private data class Service1_Foo(
        val barId: String? = null,
        val id: String? = null,
    )
}
