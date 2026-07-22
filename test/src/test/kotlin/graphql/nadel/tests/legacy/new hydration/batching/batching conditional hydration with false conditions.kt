package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching conditional hydration with false conditions` : NadelLegacyIntegrationTest(
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
                          predicate: { equals: "thatType" }
                        }
                      }
                    )
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
                        Service1_Foo(barIds = listOf("barId1", "barId2"), type = "thatOtherType")
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
