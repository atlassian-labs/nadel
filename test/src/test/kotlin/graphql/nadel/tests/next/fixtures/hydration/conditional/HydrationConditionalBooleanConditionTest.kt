package graphql.nadel.tests.next.fixtures.hydration.conditional

import graphql.nadel.tests.next.NadelIntegrationTest

class HydrationConditionalBooleanConditionTest : NadelIntegrationTest(
    query = """
        query {
          foo {
            matchingBar {
              name
            }
            nonMatchingBar {
              name
            }
          }
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "service1",
            overallSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  id: ID
                  shouldHydrate: Boolean
                  matchingBarId: ID @hidden
                  nonMatchingBarId: ID @hidden
                  matchingBar: Bar
                    @hydrated(
                      service: "service2"
                      field: "barById"
                      arguments: [{name: "id", value: "$source.matchingBarId"}]
                      when: { result: { sourceField: "shouldHydrate", predicate: { equals: true } } }
                    )
                  nonMatchingBar: Bar
                    @hydrated(
                      service: "service2"
                      field: "barById"
                      arguments: [{name: "id", value: "$source.nonMatchingBarId"}]
                      when: { result: { sourceField: "shouldHydrate", predicate: { equals: false } } }
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Foo(
                    val id: String,
                    val shouldHydrate: Boolean,
                    val matchingBarId: String,
                    val nonMatchingBarId: String,
                )

                val foo = Foo(
                    id = "foo-id",
                    shouldHydrate = true,
                    matchingBarId = "matching-bar-id",
                    nonMatchingBarId = "non-matching-bar-id",
                )

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("foo") { foo }
                    }
            },
        ),
        Service(
            name = "service2",
            overallSchema = """
                type Query {
                  barById(id: ID): Bar
                }
                type Bar {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Bar(
                    val id: String,
                    val name: String,
                )

                val barsById = listOf(
                    Bar(id = "matching-bar-id", name = "Matching Bar"),
                    Bar(id = "non-matching-bar-id", name = "Non Matching Bar"),
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("barById") {
                            barsById[it.getArgument<String>("id")!!]
                        }
                    }
            },
        ),
    ),
)
