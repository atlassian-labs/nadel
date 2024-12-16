package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.ServiceLike
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelDynamicServiceResolutionResult
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook
import graphql.normalized.ExecutableNormalizedField

open class PartitionFollowedByRenamedTest : NadelIntegrationTest(
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
  things(ids: [ID!]! ): [Thing]  @partition(pathToPartitionArg: ["ids"])
}

type Thing {
  id: ID!
  name: String @renamed(from: "underlyingName")
}
            """.trimIndent(),
            underlyingSchema = """
type Query {
  things(ids: [ID!]! ): [Thing]
}

type Thing {
  id: ID!
  underlyingName: String
}
            """,
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
                                        "underlyingName" to parts[0].uppercase(),
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
                    override fun resolveServiceForField(
                        services: List<ServiceLike>,
                        executableNormalizedField: ExecutableNormalizedField,
                    ): NadelDynamicServiceResolutionResult {
                        throw UnsupportedOperationException()
                    }

                    override fun partitionTransformerHook(): NadelPartitionTransformHook {
                        return RoutingBasedPartitionTransformHook()
                    }
                }
            )
    }
}
