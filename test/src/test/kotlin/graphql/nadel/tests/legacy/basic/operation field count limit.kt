package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `operation field count limit` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            __typename
            name
            child {
              name
            }
          }
          __typename
          bar: foo {
              barName: name
              barChild: child {
                barTypeName: __typename
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
                }
                type Foo {
                  name: String
                  child: Foo
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  name: String
                  child: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class Service_Foo(
        val name: String? = null,
        val child: Service_Foo? = null,
    )
}
