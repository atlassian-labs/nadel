package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Different selections cannot be pooled into one backing field without changing GraphQL execution
 * semantics. They can still share one service request by using an aliased backing field per
 * selection.
 *
 * The same source ID and child response key are intentional: without distinct backing aliases,
 * GraphQL would merge the roots and the conflicting `avatar` arguments would be invalid.
 */
open class BatchHydrationCoalescingDifferentSelectionsTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            assigneeUser: assignee {
              picture: avatar(size: 32)
            }
            reporterUser: reporter {
              picture: avatar(size: 64)
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
                                    reporterId = "ari:cloud:identity::user/1",
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
                  avatar(size: Int!): String!
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
                    .type("User") { type ->
                        type.dataFetcher("avatar") { environment ->
                            val user = environment.getSource<User>()!!
                            val size = environment.getArgument<Int>("size")
                            "${user.name}-$size"
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
