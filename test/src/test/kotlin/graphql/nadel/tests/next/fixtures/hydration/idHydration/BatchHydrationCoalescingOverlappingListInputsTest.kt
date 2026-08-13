package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Overlapping list consumers pool and deduplicate backing inputs while retaining each consumer's
 * original duplicate occurrences and ordering in the final result.
 */
class BatchHydrationCoalescingOverlappingListInputsTest : NadelIntegrationTest(
    query = """
        query {
          issue {
            reviewers {
              name
            }
            approvers {
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
                  reviewerIds: [ID]
                  approverIds: [ID]
                  reviewers: [User] @idHydrated(idField: "reviewerIds")
                  approvers: [User] @idHydrated(idField: "approverIds")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val reviewerIds: List<String>,
                    val approverIds: List<String>,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issue") {
                            Issue(
                                reviewerIds = listOf(
                                    "ari:cloud:identity::user/1",
                                    "ari:cloud:identity::user/2",
                                    "ari:cloud:identity::user/1",
                                ),
                                approverIds = listOf(
                                    "ari:cloud:identity::user/2",
                                    "ari:cloud:identity::user/3",
                                    "ari:cloud:identity::user/2",
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
                type User @defaultHydration(field: "usersByIds", idArgument: "ids", identifiedBy: "id") {
                  id: ID!
                  name: String!
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
            .batchHydrationCoalescing(NadelBatchHydrationCoalescingHint { true })
    }
}
