package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook

open class PartitionWithTypeNameTest : NadelIntegrationTest(
    query = """
      query getPartitionedThings{
        __typename
        things(ids: [
            "thing-1:partition-A", "thing-2:partition-B", "thing-3:partition-A", "thing-4:partition-B",
            "thing-5:partition-C", "thing-6:partition-D", "thing-7:partition-C", "thing-8:partition-D"
        ]) {
          __typename
          id
          name
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "things_service",
            overallSchema = """
                type Query {
                  things(ids: [ID!]!): [Thing] @partition(pathToPartitionArg: ["ids"])
                }
                type Thing {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("things") { env ->
                                val ids = env.getArgument<List<String>>("ids")

                                ids!!.map { id ->
                                    val parts = id.split(":")
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
