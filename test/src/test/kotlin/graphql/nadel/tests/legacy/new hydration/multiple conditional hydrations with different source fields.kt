package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `multiple conditional hydrations with different source fields` : NadelLegacyIntegrationTest(
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
                  type: Int
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: ID
                  name: String
                  type: Int
                  class: String
                }
                union Bars = Bar
                type Query {
                  barById(id: ID): Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("id") == "BAR_A") {
                            Service2_Bar(name = "Bar A")
                        } else if (env.getArgument<Any?>("id") == "BAR_B") {
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
                  type: Int
                  class: String @hidden
                  bar: Bars
                  @hydrated(
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
                          predicate: { equals: 1 }
                      }
                    }
                  )
                  @hydrated(
                    field: "barById" 
                    arguments: [
                      {
                        name: "id" 
                        value: "BAR_B"
                      }
                    ]
                    when: {
                      result: {
                          sourceField: "class"
                          predicate: { startsWith: "A" }
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
                  type: Int
                  class: String
                }
                type Query {
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Service1_Foo(`class` = "ABC_123", type = 2)
                    }
                }
            },
        ),
    ),
) {
    private data class Service2_Bar(
        val id: String? = null,
        val name: String? = null,
        val type: Int? = null,
        val `class`: String? = null,
    ) : Service2_Bars

    private sealed interface Service2_Bars

    private data class Service1_Foo(
        val barId: String? = null,
        val id: String? = null,
        val type: Int? = null,
        val `class`: String? = null,
    )
}
