package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Two fields derived from the same type-level default hydration request the same selection.
 *
 * With coalescing enabled, the Identity snapshot should contain one `usersByIds` field whose
 * inputs are pooled and deduplicated across both consumers.
 */
open class BatchHydrationCoalescingEqualSelectionsTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            assignee {
              name
            }
            reporter {
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
                                Issue(
                                    assigneeId = "ari:cloud:identity::user/1",
                                    reporterId = "ari:cloud:identity::user/3",
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
                type User @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 100, identifiedBy: "id") {
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
