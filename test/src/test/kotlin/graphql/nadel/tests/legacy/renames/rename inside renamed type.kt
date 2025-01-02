package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `rename inside renamed type` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            __typename
            title
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service1",
            overallSchema = """
                type Query {
                  foo: FooX
                }
                type FooX @renamed(from: "Foo") {
                  id: ID
                  title: ID @renamed(from: "barId")
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
                        Service1_Foo(barId = "Bar1")
                    }
                }
            },
        ),
    ),
) {
    private data class Service1_Foo(
        val barId: String? = null,
        val id: String? = null,
    )
}
