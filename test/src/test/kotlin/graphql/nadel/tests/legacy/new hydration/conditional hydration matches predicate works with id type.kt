package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `conditional hydration matches predicate works with id type` : NadelLegacyIntegrationTest(
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
                  barById(id: ID): Bar
                }
                type Bar {
                  id: ID
                  name: String
                  type: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: ID
                  name: String
                  type: ID
                }
                union Bars = Bar
                type Query {
                  barById(id: ID): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("id") == "BAR_B") {
                            Service2_Bar(name = "Bar B")
                        } else {
                            null
                        }
                    }
                }
                wiring.type("Bars") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
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
                  type: ID
                  bar: Bars
                  @hydrated(
                    service: "service2"
                    field: "barById"
                    arguments: [
                      {
                        name: "id" 
                        value: "BAR_A"
                      }
                    ]
                    when: {
                      result: {
                        sourceField: "type"
                        predicate: { matches: "nope" }
                      }
                    }
                  )
                  @hydrated(
                    service: "service2" 
                    field: "barById" 
                    arguments: [
                      {
                        name: "id" 
                        value: "BAR_B"
                      }
                    ]
                    when: {
                      result: {
                        sourceField: "type"
                        predicate: { matches: "[a-z]+_B"}
                      }
                    }
                  )
                }
                union Bars = Bar
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  barId: ID
                  id: ID
                  type: ID
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(type = "type_B")
                    }
                }
            },
        ),
    ),
) {
    private data class Service2_Bar(
        val id: String? = null,
        val name: String? = null,
        val type: String? = null,
    ) : Service2_Bars

    private sealed interface Service2_Bars

    private data class Service1_Foo(
        val barId: String? = null,
        val id: String? = null,
        val type: String? = null,
    )
}
