package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `basic hydration with static arg float` : NadelLegacyIntegrationTest(
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
                  barWithSomeFloat(someFloat: Float): Bar
                }
                type Bar {
                  id: ID
                  name: String
                  someFloat: Float
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: ID
                  name: String
                  someFloat: Float
                }
                type Query {
                  barWithSomeFloat(someFloat: Float): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barWithSomeFloat") { env ->
                        if (env.getArgument<Any?>("someFloat") == 123.45) {
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
                    field: "barWithSomeFloat"
                    arguments: [{name: "someFloat" value: 123.45}]
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
                        Service1_Foo()
                    }
                }
            },
        ),
    ),
) {
    private data class Service2_Bar(
        val id: String? = null,
        val name: String? = null,
        val someFloat: Double? = null,
    )

    private data class Service1_Foo(
        val barId: String? = null,
        val id: String? = null,
    )
}
