package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `custom json scalar as input type` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo(input: {something: true answer: "42"}) {
            id
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
                }
                scalar JSON
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
    ),
) {
    private data class Service_Foo(
        val id: String? = null,
    )
}
