package graphql.nadel.tests.next.fixtures.partition

import graphql.language.ScalarValue
import graphql.language.StringValue
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.partition.NadelFieldPartitionContext
import graphql.nadel.engine.transform.partition.NadelPartitionKeyExtractor
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInputValueDefinition
import java.util.function.Predicate

open class PartitionWithConflictingRoutingValuesThatAreFilteredTest : NadelIntegrationTest(
    query = """
      query getPartitionedConnections {
        things(filter: { thingsIds: [
            { to: "thing-1-primary:partition-A", from: "thing-1-secondary:partition-B" }, 
            { to: "thing-2-secondary:partition-A", from: "thing-2-primary:partition-B" },
            { to: "thing-3-primary:partition-B", from: "thing-3-secondary:partition-A" },
            { to: "thing-4-secondary:partition-B", from: "thing-4-primary:partition-A" } 
        ]}) {
          to {
              id
              name
          }
          from {
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
type Query {
  things(filter: ThingsFilter): [Connection] @partition(pathToPartitionArg: ["filter", "thingsIds"])
}

input ThingsFilter {
  thingsIds: [ThingId!]!
}

input ThingId {
    to: ID!
    from: ID!
}

type Connection {
    to: Thing
    from: Thing
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
                                    val to = thingId["to"]!!.split(":")[0]
                                    val from = thingId["from"]!!.split(":")[0]
                                    mapOf(
                                        "to" to mapOf(
                                            "id" to to,
                                            "name" to to.uppercase(),
                                        ),
                                        "from" to mapOf(
                                            "id" to from,
                                            "name" to from.uppercase(),
                                        )
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
                        return object : NadelPartitionTransformHook {
                            override fun getFieldPartitionContext(
                                executionContext: NadelExecutionContext,
                                serviceExecutionContext: NadelServiceExecutionContext,
                                executionBlueprint: NadelOverallExecutionBlueprint,
                                services: Map<String, graphql.nadel.Service>,
                                service: graphql.nadel.Service,
                                overallField: ExecutableNormalizedField,
                                hydrationDetails: ServiceExecutionHydrationDetails?,
                            ): NadelFieldPartitionContext {
                                return TestPartitionContext
                            }

                            override fun getPartitionKeyExtractor(): NadelPartitionKeyExtractor {
                                return object : NadelPartitionKeyExtractor {
                                    override fun getPartitionKey(
                                        scalarValue: ScalarValue<*>,
                                        inputValueDef: GraphQLInputValueDefinition,
                                        context: NadelFieldPartitionContext
                                    ): String? {
                                        return if (scalarValue !is StringValue) {
                                            null
                                        } else {
                                            if (scalarValue.value.contains(":").not()) {
                                                null
                                            } else if ((context as TestPartitionContext).getPredicate().test(scalarValue.value as String)
                                                    .not()
                                            ) {
                                                null
                                            } else {
                                                scalarValue.value.split(":")[1]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
    }
}

object TestPartitionContext: NadelFieldPartitionContext() {
    fun getPredicate(): Predicate<String> {
        return Predicate { partitionValue ->
            partitionValue.contains("primary")
        }
    }
}
