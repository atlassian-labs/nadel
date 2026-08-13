package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Structurally identical field-declared batch hydrations must not opt into type-level coalescing.
 *
 * Even with the hint enabled, the Identity snapshot should retain two independent service calls.
 */
class BatchHydrationCoalescingFieldDeclaredIsolationTest : NadelIntegrationTest(
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
                  userId: ID
                  assignee: User
                    @hydrated(
                      service: "Identity"
                      field: "usersByIds"
                      arguments: [{name: "ids", value: "$source.userId"}]
                      identifiedBy: "id"
                      batchSize: 100
                    )
                  reporter: User
                    @hydrated(
                      service: "Identity"
                      field: "usersByIds"
                      arguments: [{name: "ids", value: "$source.userId"}]
                      identifiedBy: "id"
                      batchSize: 100
                    )
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val userId: String,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issue") {
                            Issue(userId = "ari:cloud:identity::user/1")
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
