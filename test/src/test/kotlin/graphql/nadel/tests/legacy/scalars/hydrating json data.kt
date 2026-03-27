package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `hydrating json data` : NadelLegacyIntegrationTest(
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
                  foo: Foo @hydrated(
                    field: "otherFoo"
                    arguments: [{ name: "id" value: "${'$'}source.id" }]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo(input: JSON): Foo
                }
                type Foo {
                  id: ID!
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        if (env.getArgument<Any?>("input") == mapOf("something" to true, "answer" to "42")) {
                            Service_Foo(id = "10000")
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
                  otherFoo(id: ID!): Foo @hidden
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  otherFoo(id: ID!): Foo
                }
                type Foo {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("otherFoo") { env ->
                        if (env.getArgument<Any?>("id") == "10000") {
                            Baz_Foo(id = "-10000")
                        } else {
                            null
                        }
                    }
                }
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
    )

    private data class Baz_Foo(
        val id: String? = null,
    )
}
