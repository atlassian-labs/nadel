package graphql.nadel.tests.next.fixtures.hydration.conditional

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver

class HydrationConditionalMultipleEnumConditionsTest : NadelIntegrationTest(
    query = """
        query {
          foo {
            bar {
              ... on Bar {
                name
              }
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
                enum FooType {
                  BUG
                  STORY
                }
                type Foo {
                  id: ID
                  type: FooType
                  barId: ID @hidden
                  bar: Bars
                    @hydrated(
                      service: "service2"
                      field: "bugBarById"
                      arguments: [{name: "id", value: "$source.barId"}]
                      when: { result: { sourceField: "type", predicate: { equals: "BUG" } } }
                    )
                    @hydrated(
                      service: "service2"
                      field: "storyBarById"
                      arguments: [{name: "id", value: "$source.barId"}]
                      when: { result: { sourceField: "type", predicate: { equals: "STORY" } } }
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Foo(
                    val id: String,
                    val type: String,
                    val barId: String,
                )

                val foo = Foo(
                    id = "foo-id",
                    type = "STORY",
                    barId = "bar-id",
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
                  bugBarById(id: ID): Bar
                  storyBarById(id: ID): Bar
                }
                type Bar {
                  id: ID
                  name: String
                }
                union Bars = Bar
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Bar(
                    val id: String,
                    val name: String,
                )

                val bugBarsById = listOf(
                    Bar(id = "bar-id", name = "Bug Bar"),
                ).associateBy { it.id }
                val storyBarsById = listOf(
                    Bar(id = "bar-id", name = "Story Bar"),
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("bugBarById") {
                                bugBarsById[it.getArgument<String>("id")!!]
                            }
                            .dataFetcher("storyBarById") {
                                storyBarsById[it.getArgument<String>("id")!!]
                            }
                    }
                    .type("Bars") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
            },
        ),
    ),
)
