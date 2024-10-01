package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook
import graphql.nadel.tests.next.fixtures.partition.hooks.ThingsDataFetcherFactory

open class NoPartitionTest : NadelIntegrationTest(
    query = """
      query getPartitionedThings{
        things(ids: [
            "thing-1:partition-A", "thing-2:partition-A", "thing-3:partition-A", "thing-4:partition-A",
            "thing-5:partition-A", "thing-6:partition-A", "thing-7:partition-A", "thing-8:partition-A"
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
directive @routing (pathToSplitPoint: [String!]!) on FIELD_DEFINITION

type Query {
  things(ids: [ID!]! ): [Thing]  @routing(pathToSplitPoint: ["ids"])
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
