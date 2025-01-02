package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `fragment referenced twice from inside query and inside another fragment` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            id
            ...F2
            ...F1
          }
        }
        fragment F2 on Bar {
          id
        }
        fragment F1 on Bar {
          id
          ...F2
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Foo",
            overallSchema = """
                type Query {
                  foo: Bar
                }
                type Bar {
                  id: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  id: String
                }
                type Query {
                  foo: Bar
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Foo_Bar(id = "ID")
                    }
                }
            },
        ),
    ),
) {
    private data class Foo_Bar(
        val id: String? = null,
    )
}
