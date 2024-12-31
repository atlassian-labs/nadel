package graphql.nadel.tests.legacy.`skip-include-fields`.hydration

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on batch hydrated field` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foos {
            test @include(if: ${'$'}test) {
              id
            }
          }
        }
    """.trimIndent(),
    variables = mapOf("test" to false),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foos: [Foo]
                  tests(ids: [ID]): [Test]
                }
                type Foo {
                  test: Test @hydrated(
                    service: "service"
                    field: "tests"
                    arguments: [
                      {name: "ids" value: "${'$'}source.id"}
                    ]
                    identifiedBy: "id"
                  )
                }
                type Test {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foos: [Foo]
                  tests(ids: [ID]): [Test]
                }
                type Foo {
                  id: String
                }
                type Test {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foos") { env ->
                        listOf(Service_Foo())
                    }
                }
            },
        ),
    ),
) {
    private data class Service_Foo(
        val id: String? = null,
    )

    private data class Service_Test(
        val id: String? = null,
    )
}
