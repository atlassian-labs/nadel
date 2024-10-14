package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook
import graphql.nadel.tests.next.fixtures.partition.hooks.ThingsDataFetcherFactory

open class PartialPartitionTest : NadelIntegrationTest(
    query = """
      query getPartitionedThings {
        api {
            things(ids: ["thing-1:partition-A", "thing-2:partition-B", "thing-3:partition-A", "thing-4:partition-B"]) {
              id
              name
            }
            stuff(id: "Stuff-1") {
              id
              name
            }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "things_service",
            overallSchema = """

type Query {
  api: Api @namespace
}

type Api {
  things(ids: [ID!]! ): [Thing]  @partition(pathToSplitPoint: ["ids"])
  stuff(id: ID!): Stuff
}

type Thing {
  id: ID!
  name: String
}

type Stuff {
  id: ID!
  name: String
}
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type.dataFetcher("api") { _ -> Any() }
                    }
                    .type("Api") { type ->
                        type
                            .dataFetcher("things", ThingsDataFetcherFactory.makeIdsDataFetcher())
                            .dataFetcher("stuff") { env ->
                                val id = env.getArgument<String>("id")!!

                                mapOf(
                                    "id" to id,
                                    "name" to id.uppercase(),
                                )
                            }
                    }
            },
        ),
    ),
) {

    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun partitionTransformerHook(): NadelPartitionTransformHook {
                        return RoutingBasedPartitionTransformHook()
                    }
                }
            )
    }
}
