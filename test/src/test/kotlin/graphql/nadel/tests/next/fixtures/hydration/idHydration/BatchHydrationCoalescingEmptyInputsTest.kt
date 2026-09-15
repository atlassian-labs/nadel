package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * A compatible group with only null scalar inputs and an empty list has nothing to query. It
 * should still set the correct null/list result shapes without making an Identity service call.
 */
class BatchHydrationCoalescingEmptyInputsTest : NadelIntegrationTest(
    query = """
        query {
          issue {
            assignee {
              name
            }
            reporter {
              name
            }
            reviewers {
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
                  reviewerIds: [ID]
                  assignee: User @idHydrated(idField: "assigneeId")
                  reporter: User @idHydrated(idField: "reporterId")
                  reviewers: [User] @idHydrated(idField: "reviewerIds")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val assigneeId: String?,
                    val reporterId: String?,
                    val reviewerIds: List<String>,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issue") {
                            Issue(
                                assigneeId = null,
                                reporterId = null,
                                reviewerIds = emptyList(),
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
                type User @defaultHydration(field: "usersByIds", idArgument: "ids", identifiedBy: "id") {
                  id: ID!
                  name: String!
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                runtime
                    .type("Query") { type ->
                        type.dataFetcher("usersByIds") {
                            error("The backing field must not execute for empty pooled inputs")
                        }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchHydrationCoalescing(NadelBatchHydrationCoalescingHint { true })
    }
}
