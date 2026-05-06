package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest

class ScalarIndexHydrationTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            id
            key
            linkedCount
          }
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issues: [Issue]
                  linkedCounts(ids: [ID!]!): [Int]
                }
                type Issue {
                  id: ID!
                  key: String
                  assigneeId: ID @hidden
                  linkedCount: Int @hydrated(field: "linkedCounts", arguments: [{name: "ids", value: "$source.id"}], indexed: true)
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val key: String,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") {
                                List(10) { index ->
                                    val id = index + 1
                                    Issue(
                                        id = id.toString(),
                                        key = "GQLGW-${id}",
                                    )
                                }
                            }
                            .dataFetcher("linkedCounts") { env ->
                                env.getArgument<List<String>>("ids")!!
                                    .map { id ->
                                        (id.toIntOrNull() ?: 0) * 2
                                    }
                            }
                    }
            },
        ),
    ),
)
