package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.ServiceLike
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelDynamicServiceResolutionResult
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook
import graphql.normalized.ExecutableNormalizedField

open class PartitionTypeWithMultipleRoutingFieldsTest : NadelIntegrationTest(
    query = """
      query getPartitionedThings {
        things(filter: { thingsIds: [
            { primaryId: "thing-1-primary:partition-A", secondaryId: "thing-1-secondary" }, 
            { primaryId: "thing-2-same-partition:partition-B", secondaryId: "thing-2-secondary-same-partition:partition-B" },
            { primaryId: "thing-3-primary-no-partition", secondaryId: "thing-3-secondary:partition-A" },
            { primaryId: "thing-4-primary:partition-B", secondaryId: "thing-4-secondary" } 
        ]}) {
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
  things(filter: ThingsFilter): [Thing] @partition(pathToPartitionArg: ["filter", "thingsIds"])
}

input ThingsFilter {
  thingsIds: [ThingId!]!
}

input ThingId {
    primaryId: ID!
    secondaryId: ID!
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
                                val filter = env.getArgument<Map<String, List<Map<String, String>>>>("filter")!!
                                val thingsIds = filter["thingsIds"]!!

                                thingsIds.map { thingId ->
                                    val parts = thingId["primaryId"]!!.split(":")
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
