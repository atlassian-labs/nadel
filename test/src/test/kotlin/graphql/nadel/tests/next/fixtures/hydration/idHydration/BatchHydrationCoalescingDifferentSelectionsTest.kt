package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Two different selections share one bounded Identity request using an aliased backing root per
 * selection. The error in the first root must remain confined to the assignee occurrence while
 * the second root retains its successful data.
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

                class AvatarUnavailableError(path: List<Any>) : NadelGraphQLErrorException(
                    message = "Avatar unavailable",
                    path = path,
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
                            if (size == 32) {
                                throw AvatarUnavailableError(
                                    path = environment.executionStepInfo.path.toList(),
                                )
                            }
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

    override fun assert(
        result: ExecutionResult,
        incrementalResults: List<DelayedIncrementalPartialResult>?,
    ) {
        assertNull(incrementalResults)

        val specification = result.toSpecification()
        @Suppress("UNCHECKED_CAST")
        val errors = specification["errors"] as List<Map<String, Any?>>
        assertEquals(
            expected = listOf("issues", 0, "assigneeUser", "picture"),
            actual = errors.single()["path"],
        )

        @Suppress("UNCHECKED_CAST")
        val data = specification["data"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val issue = (data["issues"] as List<Map<String, Any?>>).single()
        @Suppress("UNCHECKED_CAST")
        val reporter = issue["reporterUser"] as Map<String, Any?>
        assertEquals(expected = "One-64", actual = reporter["picture"])
    }
}
