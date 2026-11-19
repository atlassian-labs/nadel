package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Pooled inputs are deduplicated before batch-size chunking, while each consumer retains its own
 * scalar/list/null cardinality and list ordering.
 *
 * The two backing batches contain two and one argument respectively. Since their combined
 * cardinality exceeds `batchSize: 2`, the Identity snapshot should contain two service requests.
 * The repeated list IDs must still appear repeatedly in the overall result.
 */
class BatchHydrationCoalescingMixedCardinalityTest : NadelIntegrationTest(
    query = """
        query {
          issue {
            primary {
              name
            }
            summary
            reviewers {
              name
            }
            absent {
              name
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "Jira",
            overallSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  primaryId: ID
                  reviewerIds: [ID]
                  absentId: ID
                  summary: String
                  primary: User @idHydrated(idField: "primaryId")
                  reviewers: [User] @idHydrated(idField: "reviewerIds")
                  absent: User @idHydrated(idField: "absentId")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val primaryId: String,
                    val reviewerIds: List<String>,
                    val absentId: String?,
                    val summary: String,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issue") {
                            Issue(
                                primaryId = "ari:cloud:identity::user/1",
                                reviewerIds = listOf(
                                    "ari:cloud:identity::user/3",
                                    "ari:cloud:identity::user/1",
                                    "ari:cloud:identity::user/3",
                                    "ari:cloud:identity::user/2",
                                ),
                                absentId = null,
                                summary = "Between hydrated fields",
                            )
                        }
                    }
            },
        ),
        Service(
            name = "Identity",
            overallSchema = """
                type Query {
                  usersByIds(ids: [ID!]!): [User]
                }
                type User @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 2, identifiedBy: "id") {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val id: String,
                    val name: String,
                )

                val usersById = listOf(
                    User(id = "ari:cloud:identity::user/1", name = "One"),
                    User(id = "ari:cloud:identity::user/2", name = "Two"),
                    User(id = "ari:cloud:identity::user/3", name = "Three"),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("usersByIds") { environment ->
                            environment.getArgument<List<String>>("ids")
                                ?.map(usersById::get)
                        }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchHydrationCoalescing(NadelBatchHydrationCoalescingHint { _ -> true })
    }
}
