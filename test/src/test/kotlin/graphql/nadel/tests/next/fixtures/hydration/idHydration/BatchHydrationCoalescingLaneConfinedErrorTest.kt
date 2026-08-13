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
 * An item error in the `name` lane must be mapped only to the assignee occurrence. The `email`
 * lane for the same source ID executes in a separate bounded downstream operation and must retain
 * its successful data.
 */
class BatchHydrationCoalescingLaneConfinedErrorTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            assignee {
              name
            }
            reporter {
              email
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
                type User @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 1, identifiedBy: "id") {
                  id: ID!
                  name: String
                  email: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val id: String,
                    val email: String,
                )

                class NameUnavailableError(path: List<Any>) : NadelGraphQLErrorException(
                    message = "Name unavailable",
                    path = path,
                )

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                        email = "one@example.com",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("usersByIds") { environment ->
                            environment.getArgument<List<String>>("ids")
                                ?.map(usersById::get)
                        }
                    }
                    .type("User") { type ->
                        type.dataFetcher("name") { environment ->
                            throw NameUnavailableError(
                                path = environment.executionStepInfo.path.toList(),
                            )
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
            expected = listOf("issues", 0, "assignee", "name"),
            actual = errors.single()["path"],
        )

        @Suppress("UNCHECKED_CAST")
        val data = specification["data"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val issue = (data["issues"] as List<Map<String, Any?>>).single()
        @Suppress("UNCHECKED_CAST")
        val reporter = issue["reporter"] as Map<String, Any?>
        assertEquals(expected = "one@example.com", actual = reporter["email"])
    }
}
