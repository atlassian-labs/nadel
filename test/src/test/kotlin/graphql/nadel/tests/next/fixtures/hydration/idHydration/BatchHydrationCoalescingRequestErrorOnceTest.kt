package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.ServiceExecution
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * An unattributable failure from one bounded backing operation is exposed once and does not
 * discard its successful sibling operation. Each operation reports only the consumer whose
 * source input it contains.
 */
class BatchHydrationCoalescingRequestErrorOnceTest : NadelIntegrationTest(
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
                        type.dataFetcher("issue") {
                            Issue(
                                assigneeId = "ari:cloud:identity::user/1",
                                reporterId = "ari:cloud:identity::user/2",
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
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(
                    val id: String,
                    val name: String,
                )

                class IdentityUnavailableError : NadelGraphQLErrorException(
                    message = "Identity unavailable",
                )

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                        name = "One",
                    ),
                    User(
                        id = "ari:cloud:identity::user/2",
                        name = "Two",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type.dataFetcher("usersByIds") { environment ->
                            val ids = environment.getArgument<List<String>>("ids").orEmpty()
                            if ("ari:cloud:identity::user/2" in ids) {
                                throw IdentityUnavailableError()
                            }
                            ids.map(usersById::get)
                        }
                    }
            },
        ),
    ),
) {
    private val capturedConsumers = ConcurrentLinkedQueue<List<List<String>>>()

    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchHydrationCoalescing(NadelBatchHydrationCoalescingHint { _ -> true })
    }

    override fun makeServiceExecution(service: Service): ServiceExecution {
        val delegate = super.makeServiceExecution(service)

        return ServiceExecution { parameters ->
            parameters.hydrationDetails
                ?.consumerDetails
                ?.map { consumer -> consumer.fieldPath }
                ?.let(capturedConsumers::add)

            delegate.execute(parameters)
        }
    }

    override fun assert(
        result: ExecutionResult,
        incrementalResults: List<DelayedIncrementalPartialResult>?,
    ) {
        @Suppress("UNCHECKED_CAST")
        val errors = result.toSpecification()["errors"] as List<Map<String, Any?>>

        assertEquals(expected = 1, actual = errors.size)
        assertEquals(expected = "Identity unavailable", actual = errors.single()["message"])
        assertNull(errors.single()["path"])
        assertEquals(
            expected = setOf(
                listOf(listOf("issue", "assignee")),
                listOf(listOf("issue", "reporter")),
            ),
            actual = capturedConsumers.toSet(),
        )
    }
}
