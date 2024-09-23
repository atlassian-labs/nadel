package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest

open class PartialPartitionTest : NadelIntegrationTest(
    query = """
      query {
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
directive @routing (pathToSplitPoint: [String!]!) on FIELD_DEFINITION

type Query {
  api: Api
}

type Api {
  things(ids: [ID!]! ): [Thing]  @routing(pathToSplitPoint: ["ids"])
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
