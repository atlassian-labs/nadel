package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Outer consumers with a hydration nested inside their selection remain isolated.
 *
 * Nested backing errors currently cross the hydration execution boundary without source
 * occurrence provenance, so pooling the outer lookups would make those errors impossible to
 * attribute safely. The nested manager fields still hydrate through ordinary nested execution.
 */
class BatchHydrationCoalescingNestedSelectionHydrationTest : NadelIntegrationTest(
    query = """
        query {
          issue {
            assignee {
              name
              manager {
                name
              }
            }
            reporter {
              name
              manager {
                name
              }
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
                        type.dataFetcher("issue") {
                            Issue(
                                assigneeId = "ari:cloud:identity::user/1",
                                reporterId = "ari:cloud:identity::user/2",
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
                  managerId: ID
                  manager: User @idHydrated(idField: "managerId")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val id: String,
                    val name: String,
                    val managerId: String?,
                )

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                        name = "One",
                        managerId = "ari:cloud:identity::user/3",
                    ),
                    User(
                        id = "ari:cloud:identity::user/2",
                        name = "Two",
                        managerId = "ari:cloud:identity::user/3",
                    ),
                    User(
                        id = "ari:cloud:identity::user/3",
                        name = "Three",
                        managerId = null,
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
