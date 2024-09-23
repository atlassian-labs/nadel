package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest

open class NamespacedPartitionTest : NadelIntegrationTest(
    query = """
      query getPartitionedThings {
        thingsApi {
            things(filter: { thingsIds: [
                { id: "thing-1:partition-A" }, 
                { id: "thing-2:partition-B" },
                { id: "thing-3:partition-A" },
                { id: "thing-4:partition-B" } 
            ]}) {
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
directive @routing (pathToSplitPoint: [String!]!) on FIELD_DEFINITION

type Query {
  thingsApi: ThingsApi
}

type ThingsApi {
  things(filter: ThingsFilter): [Thing] @routing(pathToSplitPoint: ["filter", "thingsIds"])
}

input ThingsFilter {
  thingsIds: [ThingId!]!
}

input ThingId {
    id: ID!
}

type Thing {
  id: ID!
  name: String
}
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type.dataFetcher("thingsApi") { _ -> Any() }
                    }
                    .type("ThingsApi") { type ->
                        type
                            .dataFetcher("things") { env ->
                                val filter = env.getArgument<Map<String, List<Map<String, String>>>>("filter")!!
                                val thingsIds = filter["thingsIds"]!!

                                thingsIds.map { thingId ->
                                    val parts = thingId["id"]!!.split(":")
                                    mapOf(
                                        "id" to parts[0],
                                        "name" to parts[0].uppercase(),
                                    )
                                }
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
