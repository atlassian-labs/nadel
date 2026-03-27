package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching conditional hydration with list condition field` : NadelLegacyIntegrationTest(
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
                  barsById(ids: [ID]): [Bar]
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
                  barsById(ids: [ID]): [Bar]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val barsById = listOf(
                        Service2_Bar(id = "barId2", name = "Bar2"),
                    ).associateBy { it.id }

                    type.dataFetcher("barsById") { env ->
                        env.getArgument<List<String>>("ids")?.map(barsById::get)
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
                  barIds: [ID] @hidden
                  bar: [Bar] 
                    @hydrated(
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
                          predicate: { equals: "barId2" }
                        }
                      }
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
