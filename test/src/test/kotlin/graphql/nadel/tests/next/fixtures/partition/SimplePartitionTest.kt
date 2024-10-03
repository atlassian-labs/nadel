package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook
import graphql.nadel.tests.next.fixtures.partition.hooks.ThingsDataFetcherFactory

open class SimplePartitionTest : NadelIntegrationTest(
    query = """
      query getPartitionedThings{
        things(ids: [
            "thing-1:partition-A", "thing-2:partition-B", "thing-3:partition-A", "thing-4:partition-B",
            "thing-5:partition-C", "thing-6:partition-D", "thing-7:partition-C", "thing-8:partition-D"
        ]) {
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
  things(ids: [ID!]! ): [Thing]  @partition(pathToSplitPoint: ["ids"])
}

type Thing {
  id: ID!
  name: String
}
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type.dataFetcher("things", ThingsDataFetcherFactory.makeIdsDataFetcher())
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
