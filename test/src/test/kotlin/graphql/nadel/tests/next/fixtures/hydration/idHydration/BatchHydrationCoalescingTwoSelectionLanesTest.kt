package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Two consumers derived from the same default hydration request different simple selections.
 *
 * Each root has one argument and `batchSize` is three, so the Identity snapshot should contain
 * one service call with both aliased `usersByIds` roots. This covers the below-limit case.
 */
class BatchHydrationCoalescingTwoSelectionLanesTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            assignee {
              name
            }
            reporter {
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
                  assignee: User @idHydrated(idField: "assigneeId")
                  reporter: User @idHydrated(idField: "reporterId")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val assigneeId: String,
                    val reporterId: String,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issues") {
                            listOf(
                                Issue(
                                    assigneeId = "ari:cloud:identity::user/1",
                                    reporterId = "ari:cloud:identity::user/2",
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
                type User @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 3, identifiedBy: "id") {
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

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                        name = "One",
                        email = "one@example.com",
                    ),
                    User(
                        id = "ari:cloud:identity::user/2",
                        name = "Two",
                        email = "two@example.com",
                    ),
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
