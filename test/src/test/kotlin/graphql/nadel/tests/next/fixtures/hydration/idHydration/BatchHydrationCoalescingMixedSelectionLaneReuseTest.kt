package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Two equal selections reuse one pooled lane while a third selection uses a second lane.
 *
 * The `name` root receives both assignee and reporter IDs, while the `email` root receives the
 * creator ID. Their combined cardinality exceeds `batchSize: 2`, so the Identity snapshot should
 * contain two bounded service operations.
 */
class BatchHydrationCoalescingMixedSelectionLaneReuseTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            assignee {
              name
            }
            reporter {
              name
            }
            creator {
              email
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "Jira",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  assigneeId: ID
                  reporterId: ID
                  creatorId: ID
                  assignee: User @idHydrated(idField: "assigneeId")
                  reporter: User @idHydrated(idField: "reporterId")
                  creator: User @idHydrated(idField: "creatorId")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val assigneeId: String,
                    val reporterId: String,
                    val creatorId: String,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issues") {
                            listOf(
                                Issue(
                                    assigneeId = "ari:cloud:identity::user/1",
                                    reporterId = "ari:cloud:identity::user/2",
                                    creatorId = "ari:cloud:identity::user/3",
                                ),
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
                  email: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val id: String,
                    val name: String,
                    val email: String,
                )

                val usersById = (1..3)
                    .map { ordinal ->
                        User(
                            id = "ari:cloud:identity::user/$ordinal",
                            name = "User $ordinal",
                            email = "user$ordinal@example.com",
                        )
                    }
                    .strictAssociateBy { it.id }

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
