package graphql.nadel.tests.legacy.`skip-include-fields`.hydration

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `handles include directive on field with batch hydrated parent` : NadelLegacyIntegrationTest(
    query = """
        query (${'$'}test: Boolean!) {
          foos {
            test {
              id @include(if: ${'$'}test)
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
                    val testById = listOf(
                        Service_Test(id = "Foo-4"),
                        Service_Test(id = "Foo-3"),
                    ).associateBy { it.id }

                    type
                        .dataFetcher("foos") { env ->
                            listOf(Service_Foo(id = "Foo-3"), Service_Foo(id = "Foo-4"))
                        }
                        .dataFetcher("tests") { env ->
                            env.getArgument<List<String>>("ids")?.map(testById::get)
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
