package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook
import graphql.nadel.tests.next.fixtures.partition.hooks.ThingsDataFetcherFactory
import kotlin.test.Ignore

@Ignore("Partition on fields that were renamed is not supported yet.")
open class PartitionOnRenamedFieldTest : NadelIntegrationTest(
    query = """
      query getPartitionedThings{
        renamedEcho
        renamedThings(ids: [
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
  renamedThings(ids: [ID!]! ): [Thing]  @partition(pathToSplitPoint: ["ids"]) @renamed(from: "things")
  renamedEcho: String @renamed(from: "echo")
}

type Thing {
  id: ID!
  name: String
}
            """.trimIndent(),
            underlyingSchema = """
type Query {
  things(ids: [ID!]! ): [Thing]
  echo: String
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
                        type.dataFetcher("echo") { _ -> "echo" }
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
