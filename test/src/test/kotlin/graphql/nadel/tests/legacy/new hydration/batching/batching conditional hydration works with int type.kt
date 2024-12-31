package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching conditional hydration works with int type` : NadelLegacyIntegrationTest(
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
                  barsById(ids: [Int]): [Bar]
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
                  barsById(ids: [Int]): [Bar]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barsById") { env ->
                        if (env.getArgument<Any?>("ids") == listOf(2)) {
                            listOf(Service2_Bar(id = 2, name = "Bar2"))
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
                  id: Int
                  barIds: [Int] @hidden
                  bar: [Bar] 
                    @hydrated(
                      service: "service2"
                      field: "barsById"
                      arguments: [
                        {
                          name: "ids"
                          value: "${'$'}source.barIds"
                        }
                      ]
                      when: {
                        result: {
                          sourceField: "barIds"
                          predicate: { equals: 2 }
                        }
                      }
                    )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barIds: [Int]
                  id: Int
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(barIds = listOf(1, 2, 3))
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
        val barIds: List<Int?>? = null,
        val id: Int? = null,
    )
}
