package graphql.nadel.tests.next.fixtures.hydration.conditional

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver

class BatchHydrationConditionalEnumConditionTest : NadelIntegrationTest(
    query = """
        query {
          foo {
            bars {
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
                  barIds: [ID] @hidden
                  bars: [Bars]
                    @hydrated(
                      service: "service2"
                      field: "bugBarsById"
                      arguments: [{name: "ids", value: "$source.barIds"}]
                      when: { result: { sourceField: "type", predicate: { equals: "BUG" } } }
                    )
                    @hydrated(
                      service: "service2"
                      field: "storyBarsById"
                      arguments: [{name: "ids", value: "$source.barIds"}]
                      when: { result: { sourceField: "type", predicate: { equals: "STORY" } } }
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Foo(
                    val id: String,
                    val type: String,
                    val barIds: List<String>,
                )

                val foo = Foo(
                    id = "foo-id",
                    type = "STORY",
                    barIds = listOf("bar-id-1", "bar-id-2"),
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
                  bugBarsById(ids: [ID]): [Bar]
                  storyBarsById(ids: [ID]): [Bar]
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
                    Bar(id = "bar-id-1", name = "Bug Bar 1"),
                    Bar(id = "bar-id-2", name = "Bug Bar 2"),
                ).strictAssociateBy { it.id }

                val storyBarsById = listOf(
                    Bar(id = "bar-id-1", name = "Story Bar 1"),
                    Bar(id = "bar-id-2", name = "Story Bar 2"),
                ).strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("bugBarsById") { fetchEnv ->
                                fetchEnv.getArgument<List<String>>("ids")!!.map(bugBarsById::get)
                            }
                            .dataFetcher("storyBarsById") { fetchEnv ->
                                fetchEnv.getArgument<List<String>>("ids")!!.map(storyBarsById::get)
                            }
                    }
                    .type("Bars") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
            },
        ),
    ),
)
