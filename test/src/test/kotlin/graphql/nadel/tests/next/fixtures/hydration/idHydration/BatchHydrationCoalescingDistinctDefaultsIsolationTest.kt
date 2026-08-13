package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Default hydrations on different declaring types are distinct declarations.
 *
 * Even with equal batch and identifier settings, their distinct backing outputs must remain
 * isolated.
 */
class BatchHydrationCoalescingDistinctDefaultsIsolationTest : NadelIntegrationTest(
    query = """
        query {
          issue {
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
                  issue: Issue
                }
                type Issue {
                  assigneeId: ID
                  reporterId: ID
                  assignee: AssigneeUser @idHydrated(idField: "assigneeId")
                  reporter: ReporterUser @idHydrated(idField: "reporterId")
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
                                assigneeId = "ari:cloud:identity::user/assignee",
                                reporterId = "ari:cloud:identity::user/reporter",
                            )
                        }
                    }
            },
        ),
        Service(
            name = "Identity",
            overallSchema = """
                type Query {
                  assigneeUsersByIds(ids: [ID!]!): [AssigneeUser]
                  reporterUsersByIds(ids: [ID!]!): [ReporterUser]
                }
                type AssigneeUser
                  @defaultHydration(field: "assigneeUsersByIds", idArgument: "ids", batchSize: 100, identifiedBy: "id") {
                  id: ID!
                  name: String
                }
                type ReporterUser
                  @defaultHydration(field: "reporterUsersByIds", idArgument: "ids", batchSize: 100, identifiedBy: "id") {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class AssigneeUser(
                    val id: String,
                    val name: String,
                )

                data class ReporterUser(
                    val id: String,
                    val name: String,
                )

                val assigneeUsersById = listOf(
                    AssigneeUser(
                        id = "ari:cloud:identity::user/assignee",
                        name = "Assignee",
                    ),
                ).strictAssociateBy { it.id }
                val reporterUsersById = listOf(
                    ReporterUser(
                        id = "ari:cloud:identity::user/reporter",
                        name = "Reporter",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("assigneeUsersByIds") { environment ->
                                environment.getArgument<List<String>>("ids")
                                    ?.map(assigneeUsersById::get)
                            }
                            .dataFetcher("reporterUsersByIds") { environment ->
                                environment.getArgument<List<String>>("ids")
                                    ?.map(reporterUsersById::get)
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
