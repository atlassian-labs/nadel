package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One pathful backing-field error for a shared ID must be exposed once for every affected client
 * occurrence. Internal aliases from the coalesced backing request must never escape.
 */
class BatchHydrationCoalescingErrorFanOutTest : NadelIntegrationTest(
    query = """
        query {
          issues {
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
                            List(2) {
                                Issue(
                                    assigneeId = "ari:cloud:identity::user/1",
                                    reporterId = "ari:cloud:identity::user/1",
                                )
                            }
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
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val id: String,
                )

                class NameUnavailableError(path: List<Any>) : NadelGraphQLErrorException(
                    message = "Name unavailable",
                    path = path,
                )

                val usersById = listOf(
                    User(id = "ari:cloud:identity::user/1"),
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
        assertTrue(incrementalResults == null)

        val errorPaths = result
            .toSpecification()
            .let { specification ->
                @Suppress("UNCHECKED_CAST")
                (specification["errors"] as List<Map<String, Any?>>)
                    .map { error ->
                        error["path"] as List<*>
                    }
            }

        assertEquals(
            expected = setOf(
                listOf("issues", 0, "assignee", "name"),
                listOf("issues", 0, "reporter", "name"),
                listOf("issues", 1, "assignee", "name"),
                listOf("issues", 1, "reporter", "name"),
            ),
            actual = errorPaths.toSet(),
        )
        assertEquals(expected = 4, actual = errorPaths.size)
        assertTrue(
            errorPaths
                .flatten()
                .filterIsInstance<String>()
                .none { pathSegment -> pathSegment.startsWith("batch_hydration") },
        )
    }
}
