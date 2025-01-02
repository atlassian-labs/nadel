package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching conditional hydration with multiple true conditions takes first` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            bar {
              ... on Bar {
                name
              }
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
                  othersById(ids: [ID]): [Bar]
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
                  othersById(ids: [ID]): [Bar]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val barsById = listOf(
                        Service2_Bar(id = "barId1", name = "Bar1"),
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
                  type: String
                  bar: [Bars] 
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
                          sourceField: "type"
                          predicate: { startsWith: "this" }
                        }
                      }
                    )
                    @hydrated(
                      service: "service2"
                      field: "othersById"
                      arguments: [
                        {
                          name: "ids"
                          value: "${'$'}source.barIds"
                        }
                      ]
                      when: {
                        result: {
                          sourceField: "type"
                          predicate: { equals: "thisType" }
                        }
                      }
                    )
                }
                union Bars = Bar
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barIds: [ID]
                  id: ID
                  type: String
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(barIds = listOf("barId1", "barId2"), type = "thisType")
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
        val type: String? = null,
    )
}