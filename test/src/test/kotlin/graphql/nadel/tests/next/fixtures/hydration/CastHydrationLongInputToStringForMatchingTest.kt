package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.scalars.ExtendedScalars

class CastHydrationLongInputToStringForMatchingTest : NadelIntegrationTest(
    query = """
        {
          someData {
            spaces {
              id
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                scalar Long
                type Query {
                    someData: SomeData
                    spaces(ids: [ID!]!): [Space]
                }
                type SomeData {
                    spaceIds: [Long]
                    spaces: [Space] @idHydrated(idField: "spaceIds")
                }
                type Space @defaultHydration(field: "spaces", idArgument: "ids") {
                    id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class SomeData(
                    val spaceIds: List<Long>,
                )

                data class Space(
                    val id: String,
                )

                wiring
                    .scalar(ExtendedScalars.GraphQLLong)
                    .type("Query") { type ->
                        type
                            .dataFetcher("someData") { env ->
                                SomeData(spaceIds = listOf(1, 2, 10))
                            }
                            .dataFetcher("spaces") { env ->
                                val ids = env.getArgument<List<String>>("ids")
                                ids?.map {
                                    Space(id = it)
                                }
                            }
                    }
            },
        ),
    ),
)
