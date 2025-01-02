package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `hydrating using json data as arg` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo(input: {something: true answer: "42"}) {
            foo {
              id
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foo(input: JSON): Foo
                }
                type Foo {
                  id: ID!
                  baz: JSON @hidden
                  foo: Foo @hydrated(
                    service: "Baz"
                    field: "baz"
                    arguments: [{ name: "data" value: "${'$'}source.baz" }]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo(input: JSON): Foo
                }
                type Foo {
                  id: ID!
                  baz: JSON
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        if (env.getArgument<Any?>("input") == mapOf("something" to true, "answer" to "42")) {
                            Service_Foo(
                                baz = mapOf(
                                    "id" to "102",
                                    "appConfig" to mapOf(
                                        "status" to "active",
                                        "bounce" to false,
                                    ),
                                ),
                            )
                        } else {
                            null
                        }
                    }
                }
                wiring.scalar(ExtendedScalars.Json)
            },
        ),
        Service(
            name = "Baz",
            overallSchema = """
                type Query {
                  baz(data: JSON!): Foo @hidden
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  baz(data: JSON!): Foo
                }
                type Foo {
                  id: ID!
                  baz: JSON
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("baz") { env ->
                        if (env.getArgument<Any?>("data") ==
                            mapOf(
                                "id" to "102",
                                "appConfig" to mapOf(
                                    "status" to "active",
                                    "bounce" to false,
                                ),
                            )
                        ) {
                            Baz_Foo(id = "10000")
                        } else {
                            null
                        }
                    }
                }
                wiring.scalar(ExtendedScalars.Json)
            },
        ),
        Service(
            name = "Shared",
            overallSchema = """
                scalar JSON
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.scalar(ExtendedScalars.Json)
            },
        ),
    ),
) {
    private data class Service_Foo(
        val id: String? = null,
        val baz: Any? = null,
    )

    private data class Baz_Foo(
        val id: String? = null,
        val baz: Any? = null,
    )
}
