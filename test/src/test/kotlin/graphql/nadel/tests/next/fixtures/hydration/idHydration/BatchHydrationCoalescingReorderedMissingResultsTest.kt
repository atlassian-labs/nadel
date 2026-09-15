package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Shared object-identifier matching must not depend on backing result order. A requested ID that
 * is omitted by the backing service maps to null without disturbing the other consumers.
 */
class BatchHydrationCoalescingReorderedMissingResultsTest : NadelIntegrationTest(
    query = """
        query {
          issue {
            assignee {
              name
            }
            reporter {
              name
            }
            creator {
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
                        type.dataFetcher("issue") {
                            Issue(
                                assigneeId = "ari:cloud:identity::user/1",
                                reporterId = "ari:cloud:identity::user/2",
                                creatorId = "ari:cloud:identity::user/missing",
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

                val userOne = User(id = "ari:cloud:identity::user/1", name = "One")
                val userTwo = User(id = "ari:cloud:identity::user/2", name = "Two")

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("usersByIds") { environment ->
                            val requestedIds = environment.getArgument<List<String>>("ids").orEmpty()
                            // Deliberately reverse the available results and omit the missing ID.
                            listOf(userTwo, userOne)
                                .filter { user -> user.id in requestedIds }
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
