package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `complex identified by hydration batching returns null batch` : NadelLegacyIntegrationTest(
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
                  barsById(id: [ID]): [Bar]
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
                  barsById(id: [ID]): [Bar]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barsById") { env ->
                        if (env.getArgument<Any?>("id") == listOf("barId1", "barId2", "barId3")) {
                            null
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
                  bar: [Bar] @hydrated(
                    service: "service2"
                    field: "barsById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
                    inputIdentifiedBy: [{sourceId: "barId" resultId: "id"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: [ID]
                  id: ID
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(barId = listOf("barId1", "barId2", "barId3"))
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
        val barId: List<String?>? = null,
        val id: String? = null,
    )
}
