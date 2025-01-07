package graphql.nadel.tests.legacy.schema

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `can delete fields and types` : NadelLegacyIntegrationTest(
    query = """
        query GetTypes {
          __schema {
            types {
              name
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
                  foo: Foo
                  echo: String
                }
                type Foo {
                  id: ID
                }
                type Bar {
                  id: ID
                  foo: Foo
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                  echo: String
                }
                type Foo {
                  id: ID
                }
                type Bar {
                  id: ID
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class Service_Bar(
        val id: String? = null,
        val foo: Service_Foo? = null,
    )

    private data class Service_Foo(
        val id: String? = null,
    )
}
