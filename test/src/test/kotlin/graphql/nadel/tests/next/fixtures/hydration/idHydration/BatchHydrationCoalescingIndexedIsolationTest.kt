package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Indexed batch hydrations must never opt into coalescing.
 *
 * The two hydrations have the same backing field, source inputs, and selection. Even with the
 * coalescing hint enabled, the Identity snapshot must retain two independent service calls.
 */
class BatchHydrationCoalescingIndexedIsolationTest : NadelIntegrationTest(
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
                  userIds: [ID!]!
                  reviewers: [User]
                    @hydrated(
                      service: "Identity"
                      field: "usersByIds"
                      arguments: [{name: "ids", value: "$source.userIds"}]
                      indexed: true
                      batchSize: 100
                    )
                  approvers: [User]
                    @hydrated(
                      service: "Identity"
                      field: "usersByIds"
                      arguments: [{name: "ids", value: "$source.userIds"}]
                      indexed: true
                      batchSize: 100
                    )
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val userIds: List<String>,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issue") {
                            Issue(
                                userIds = listOf(
                                    "ari:cloud:identity::user/1",
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
                type User {
                  name: String!
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val name: String,
                )

                val usersById = listOf(
                    "ari:cloud:identity::user/1" to User(name = "One"),
                    "ari:cloud:identity::user/2" to User(name = "Two"),
                ).toMap()

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
