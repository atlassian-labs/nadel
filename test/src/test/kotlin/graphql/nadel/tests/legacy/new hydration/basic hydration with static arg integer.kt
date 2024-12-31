package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `basic hydration with static arg integer` : NadelLegacyIntegrationTest(
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
                  barById(id: Int): Bar
                }
                type Bar {
                  id: Int
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: Int
                  name: String
                }
                type Query {
                  barById(id: Int): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("id") == 12_345) {
                            Service2_Bar(name = "Bar12345")
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
                    field: "barById"
                    arguments: [{name: "id" value: 12345}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: Int
                  id: ID
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo()
                    }
                }
            },
        ),
    ),
) {
    private data class Service2_Bar(
        val id: Int? = null,
        val name: String? = null,
    )

    private data class Service1_Foo(
        val barId: Int? = null,
        val id: String? = null,
    )
}
