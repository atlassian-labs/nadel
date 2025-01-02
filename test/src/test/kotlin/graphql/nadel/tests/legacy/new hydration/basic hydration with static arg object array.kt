package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `basic hydration with static arg object array` : NadelLegacyIntegrationTest(
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
                  barById(id: ID, friends: [FullName]): Bar
                }
                type Bar {
                  id: ID
                  name: String
                }
                input FullName {
                  firstName: String
                  lastName: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: ID
                  name: String
                }
                type Query {
                  barById(id: ID, friends: [FullName]): Bar
                }
                input FullName {
                  firstName: String
                  lastName: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val barById = listOf(
                        Service2_Bar(id = "barId", name = "Bar1")
                    ).associateBy { it.id }

                    val secret = listOf(
                        mapOf("firstName" to "first", "lastName" to "last"),
                        mapOf("firstName" to "first2", "lastName" to "last2"),
                        mapOf("firstName" to "first3", "lastName" to "last3"),
                    )

                    type.dataFetcher("barById") { env ->
                        if (env.getArgument<Any?>("friends") == secret) {
                            barById[env.getArgument<String>("id")]
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
                  bar: Bar @hydrated(
                    service: "service2"
                    field: "barById"
                    arguments: [
                      { name: "id" value: "${'$'}source.id" }
                      { 
                        name: "friends"
                        value: [
                          {
                            firstName: "first"
                            lastName: "last"
                          }
                          {
                            firstName: "first2"
                            lastName: "last2"
                          }
                          {
                            firstName: "first3"
                            lastName: "last3"
                          }
                        ]
                      }
                    ]
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
                        Service1_Foo(id = "barId")
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

    private data class Service2_FullName(
        val firstName: String? = null,
        val lastName: String? = null,
    )

    private data class Service1_Foo(
        val barId: String? = null,
        val id: String? = null,
    )
}
