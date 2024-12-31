package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `indexed hydrating using json data as arg` : NadelLegacyIntegrationTest(
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
                  foo(input: JSON): [Foo]
                }
                type Foo {
                  id: ID!
                  baz: JSON @hidden
                  foo: Foo @hydrated(
                    service: "Baz"
                    field: "baz"
                    arguments: [{ name: "data" value: "${'$'}source.baz" }]
                    indexed: true
                    batchSize: 2
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo(input: JSON): [Foo]
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
                            listOf(
                                Service_Foo(
                                    baz =
                                    mapOf(
                                        "id" to "102",
                                        "appConfig" to
                                            mapOf(
                                                "status" to "active",
                                                "bounce" to false,
                                            ),
                                    ),
                                ),
                                Service_Foo(
                                    baz =
                                    mapOf(
                                        "ari" to
                                            "ari:cloud:api-platform::thing/103",
                                        "config" to
                                            mapOf(
                                                "status" to "active",
                                                "bounce" to true,
                                            ),
                                    ),
                                ),
                                Service_Foo(
                                    baz =
                                    mapOf(
                                        "app-config" to
                                            mapOf(
                                                "status" to
                                                    "deactivated",
                                                "bounce" to true,
                                            ),
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
                  baz(data: [JSON!]!): [Foo] @hidden
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  baz(data: [JSON!]!): [Foo]
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
                            listOf(
                                mapOf(
                                    "id" to "102",
                                    "appConfig" to
                                        mapOf("status" to "active", "bounce" to false),
                                ),
                                mapOf(
                                    "ari" to
                                        "ari:cloud:api-platform::thing/103",
                                    "config" to
                                        mapOf(
                                            "status" to "active",
                                            "bounce"
                                                to true,
                                        ),
                                ),
                            )
                        ) {
                            listOf(Baz_Foo(id = "102"), Baz_Foo(id = "active bounce 103 thing"))
                        } else if (env.getArgument<Any?>("data") ==
                            listOf(
                                mapOf(
                                    "app-config" to
                                        mapOf(
                                            "status" to
                                                "deactivated",
                                            "bounce" to true,
                                        ),
                                ),
                            )
                        ) {
                            listOf(Baz_Foo(id = "deactivated thing"))
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
