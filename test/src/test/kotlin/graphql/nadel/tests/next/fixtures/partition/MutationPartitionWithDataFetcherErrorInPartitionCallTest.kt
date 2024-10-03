package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.Nadel
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook

open class MutationPartitionWithDataFetcherErrorInPartitionCallTest : NadelIntegrationTest(
    query = """
      mutation linkABunchOfThings {
        thingsApi {
          linkThings(linkThingsInput: {
            thinksLinked: [
              {from: "thing-1:partition-A", to: "thing-2:partition-A"},
              {from: "thing-3:partition-B", to: "thing-4:partition-B"},
              {from: "thing-5:partition-C", to: "thing-6:partition-C"},
              {from: "thing-7:partition-D", to: "thing-8:partition-D"}
            ]
          }) {
            success
            errors {
              message
            }
            linkedThings {
              id
              name
            }
          }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "things_service",
            overallSchema = """

type Query {
    echo: String
}

type Mutation {
  thingsApi: ThingsApi
}

type ThingsApi {
  linkThings(linkThingsInput: LinkThingsInput!): LinkThingsPayload 
  @partition(pathToSplitPoint: ["linkThingsInput", "thinksLinked"])
}

input LinkThingsInput {
  thinksLinked: [ThingsLinked!]!
}

input ThingsLinked {
    from: ID!
    to: ID!
}

type Thing {
  id: ID!
  name: String
}

type MutationError {
    message: String
}

type LinkThingsPayload {
    success: Boolean!
    errors: [MutationError!]
    linkedThings: [Thing]
}
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Mutation") { type ->
                        type.dataFetcher("thingsApi") { _ -> Any() }
                    }
                    .type("ThingsApi") { type ->
                        type
                            .dataFetcher("linkThings") { env ->
                                val filter =
                                    env.getArgument<Map<String, List<Map<String, String>>>>("linkThingsInput")!!
                                val thingsLinked = filter["thinksLinked"]!!

                                if(thingsLinked.any { it["from"]?.contains("partition-C") == true }) {
                                    throw RuntimeException("Could not link things in partition-C")
                                }

                                val linkedThings = thingsLinked.flatMap {
                                    listOf(
                                        mapOf(
                                            "id" to it["from"],
                                            "name" to it["from"]!!.uppercase()
                                        ),
                                        mapOf(
                                            "id" to it["to"],
                                            "name" to it["to"]!!.uppercase()
                                        )
                                    )
                                }

                                mapOf(
                                    "success" to true,
                                    "errors" to emptyList<Any>(),
                                    "linkedThings" to linkedThings
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
