package graphql.nadel.tests.legacy.`result merging`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `calls to multiple services are merged` : NadelLegacyIntegrationTest(
    query = """
        query {
          loot: foo(id: "1") {
            name
          }
          foo(id: "1") {
            name
          }
          bar(id: "1") {
            name
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "foo",
            overallSchema = """
                type Query {
                  foo(id: ID!): Foo
                }
                type Foo {
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo(id: ID!): Foo
                }
                type Foo {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        if (env.getArgument<Any?>("id") == "1" && env.field.alias == null) {
                            Foo_Foo(name = "Hello")
                        } else if (env.getArgument<Any?>("id") == "1") {
                            Foo_Foo(name = "World")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "bar",
            overallSchema = """
                type Query {
                  bar(id: ID!): Bar
                }
                type Bar {
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  bar(id: ID!): Bar
                }
                type Bar {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("bar") { env ->
                        if (env.getArgument<Any?>("id") == "1") {
                            Bar_Bar(name = "Bart")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class Foo_Foo(
        val name: String? = null,
    )

    private data class Bar_Bar(
        val name: String? = null,
    )
}
