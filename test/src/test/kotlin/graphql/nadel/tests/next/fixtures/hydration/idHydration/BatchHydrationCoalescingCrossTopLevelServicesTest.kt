package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class BatchHydrationCoalescingCrossTopLevelServicesBase(
    query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "Jira",
            overallSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  assigneeId: ID @hidden
                  assignee: User @idHydrated(idField: "assigneeId")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val assigneeId: String,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("issue") {
                            Issue(assigneeId = "ari:cloud:identity::user/jira")
                        }
                    }
            },
        ),
        Service(
            name = "Confluence",
            overallSchema = """
                type Query {
                  page: Page
                }
                type Page {
                  ownerId: ID @hidden
                  owner: User @idHydrated(idField: "ownerId")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Page(
                    val ownerId: String,
                )

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("page") {
                            Page(ownerId = "ari:cloud:identity::user/confluence")
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
                type User
                  @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 100, identifiedBy: "id") {
                  id: ID!
                  name: String
                  displayName: String
                  email: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val id: String,
                    val displayName: String,
                    val email: String,
                )

                class NameUnavailableError(path: List<Any>) : NadelGraphQLErrorException(
                    message = "Name unavailable",
                    path = path,
                )

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/jira",
                        displayName = "Jira User",
                        email = "jira@example.com",
                    ),
                    User(
                        id = "ari:cloud:identity::user/confluence",
                        displayName = "Confluence User",
                        email = "confluence@example.com",
                    ),
                ).associateBy { user -> user.id }

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
            .batchHydrationCoalescing(
                NadelBatchHydrationCoalescingHint { service ->
                    service.name == "Identity"
                },
            )
    }
}

/**
 * Independently executed Jira and Confluence root fields both hydrate the same Identity type.
 *
 * The Identity snapshot must contain one `usersByIds` request pooling both IDs. This exercises
 * request-round coalescing across top-level service executions rather than coalescing sibling
 * hydration fields found in one source result.
 */
class BatchHydrationCoalescingCrossTopLevelServicesTest :
    BatchHydrationCoalescingCrossTopLevelServicesBase(
        query = """
            query {
              issue {
                assignee {
                  name
                  displayName
                }
              }
              page {
                owner {
                  name
                  displayName
                }
              }
            }
        """.trimIndent(),
    ) {

    override fun assert(
        result: ExecutionResult,
        incrementalResults: List<DelayedIncrementalPartialResult>?,
    ) {
        assertTrue(incrementalResults == null)
        @Suppress("UNCHECKED_CAST")
        val errorPaths = (result.toSpecification()["errors"] as List<Map<String, Any?>>)
            .map { error -> error["path"] as List<*> }

        assertEquals(
            setOf(
                listOf("issue", "assignee", "name"),
                listOf("page", "owner", "name"),
            ),
            errorPaths.toSet(),
        )
        assertEquals(2, errorPaths.size)
        assertTrue(
            errorPaths.flatten().filterIsInstance<String>()
                .none { segment -> segment.startsWith("batch_hydration") || segment == "usersByIds" },
        )
    }
}
