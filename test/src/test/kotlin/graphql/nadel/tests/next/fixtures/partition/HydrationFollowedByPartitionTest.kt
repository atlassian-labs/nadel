package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook

open class HydrationFollowedByPartitionTest : NadelIntegrationTest(
    query = """
      query getViewedVideos{
        viewed {
            type
            data {
                id
                title
            }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "activities_service",
            overallSchema = """
type Query {
  viewed: [ActivityItem]
}

type ActivityItem {
  type: String
  data: Video
  @hydrated(
    service: "videos_service"
    field: "videos"
    arguments: [{name: "ids", value: "$source.dataId"}]
  )
}
            """.trimIndent(),
            underlyingSchema = """
type Query {
  viewed: [ActivityItem]
}

type ActivityItem {
  id: ID!
  type: String
  dataId: String!
}
            """,
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("viewed") { env ->
                                listOf(
                                    "video-1:partition-A",
                                    "video-2:partition-B",
                                    "video-3:partition-A",
                                    "video-4:partition-B"
                                ).map { id ->
                                    mapOf(
                                        "type" to "video",
                                        "dataId" to id,
                                    )
                                }
                            }
                    }
            },
        ),
        Service(
            name = "videos_service",
            overallSchema = """
type Query {
  videos(ids: [ID!]! ): [Video] @partition(pathToPartitionArg: ["ids"])
}

type Video {
  id: ID!
  title: String
}
            """.trimIndent(),
            underlyingSchema = """
type Query {
  videos(ids: [ID!]! ): [Video]
}

type Video {
  id: ID!
  title: String
}
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("videos") { env ->
                                val ids = env.getArgument<List<String>>("ids")

                                ids!!.map { id ->
                                    mapOf(
                                        "id" to id,
                                        "title" to id.uppercase(),
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
