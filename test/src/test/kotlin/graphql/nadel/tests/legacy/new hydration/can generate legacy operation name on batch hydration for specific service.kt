package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `can generate legacy operation name on batch hydration for specific service` : NadelLegacyIntegrationTest(
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
                    val barsById = listOf(
                        Service2_Bar(id = "barId1", name = "Bar1"),
                        Service2_Bar(id = "barId2", name = "Bar2"),
                        Service2_Bar(id = "barId3", name = "Bar3"),
                    ).associateBy { it.id }

                    type.dataFetcher("barsById") { env ->
                        env.getArgument<List<String>>("id")?.map(barsById::get)
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
                    service: "service2"
                    field: "barsById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
                    identifiedBy: "id"
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
