package graphql.nadel.tests.next.fixtures.hydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that batch hydration works correctly when data is nested under connection edges pattern.
 * 
 * This test verifies the customer scenario where issues are returned in a Relay-style connection:
 * IssueConnection -> edges -> IssueEdge -> node -> Issue
 * 
 * The test should confirm that despite the nesting, Nadel still batches all user hydration calls
 * into a single request to the identity service.
 * 
 * ## Key Verification
 * The snapshot shows that only ONE call is made to the identity service with ALL 4 user IDs:
 * ```
 * users(accountIds: ["user-alice", "user-bob", "user-charlie", "user-diana"])
 * ```
 * 
 * This proves that batch hydration works even when data is nested under edges pattern.
 */
class BatchHydrationWithConnectionEdgesTest : NadelIntegrationTest(
    query = """
        query {
          issueConnection {
            edges {
              node {
                id
                key
                assignee {
                  id
                  name
                }
              }
              cursor
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issueConnection: IssueConnection
                }
                type IssueConnection {
                  edges: [IssueEdge]
                }
                type IssueEdge {
                  node: Issue
                  cursor: String!
                }
                type Issue {
                  id: ID!
                  key: String!
                  assigneeId: ID @hidden
                  assignee: User
                    @hydrated(
                      service: "identity"
                      field: "users"
                      arguments: [{name: "accountIds", value: "$source.assigneeId"}]
                      identifiedBy: "id"
                      batchSize: 90
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val key: String,
                    val assigneeId: String,
                )
                
                data class IssueEdge(
                    val node: Issue,
                    val cursor: String,
                )
                
                data class IssueConnection(
                    val edges: List<IssueEdge>,
                )

                val issues = listOf(
                    Issue(
                        id = "issue-1",
                        key = "PROJ-1",
                        assigneeId = "user-alice",
                    ),
                    Issue(
                        id = "issue-2",
                        key = "PROJ-2",
                        assigneeId = "user-bob",
                    ),
                    Issue(
                        id = "issue-3",
                        key = "PROJ-3",
                        assigneeId = "user-charlie",
                    ),
                    Issue(
                        id = "issue-4",
                        key = "PROJ-4",
                        assigneeId = "user-diana",
                    ),
                )

                val issueConnection = IssueConnection(
                    edges = issues.map { issue ->
                        IssueEdge(
                            node = issue,
                            cursor = "cursor-${issue.id}",
                        )
                    }
                )

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issueConnection") { env ->
                            issueConnection
                        }
                    }
            }
        ),
        Service(
            name = "identity",
            overallSchema = """
                type Query {
                  users(accountIds: [ID!]!): [User]
                }
                type User {
                  id: ID!
                  name: String!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val name: String,
                )

                val users = listOf(
                    User(id = "user-alice", name = "Alice"),
                    User(id = "user-bob", name = "Bob"),
                    User(id = "user-charlie", name = "Charlie"),
                    User(id = "user-diana", name = "Diana"),
                )
                val usersByIds = users.strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("users") { env ->
                            env.getArgument<List<String>>("accountIds")!!
                                .map(usersByIds::get)
                        }
                    }
            }
        ),
    ),
) {
    /**
     * Custom assertion to explicitly verify that batch hydration is working.
     * 
     * This verifies that:
     * 1. Only ONE call is made to the identity service (not 4 separate calls)
     * 2. That single call includes ALL 4 user IDs in the batch
     */
    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        super.assert(result, incrementalResults)
        
        val snapshot = getTestSnapshot()
        
        // Verify that only ONE call was made to the identity service
        val identityServiceCalls = snapshot.calls.filter { it.service == "identity" }
        assertEquals(
            expected = 1,
            actual = identityServiceCalls.size,
            message = "Expected exactly ONE batched call to identity service, but found ${identityServiceCalls.size}"
        )
        
        // Verify that the single call contains all 4 user IDs
        val identityCall = identityServiceCalls.first()
        assertTrue(
            identityCall.query.contains("user-alice"),
            "Identity service call should include user-alice in the batch"
        )
        assertTrue(
            identityCall.query.contains("user-bob"),
            "Identity service call should include user-bob in the batch"
        )
        assertTrue(
            identityCall.query.contains("user-charlie"),
            "Identity service call should include user-charlie in the batch"
        )
        assertTrue(
            identityCall.query.contains("user-diana"),
            "Identity service call should include user-diana in the batch"
        )
        
        println("✅ VERIFIED: Batch hydration works with connection/edges pattern!")
        println("   - Only 1 call to identity service (not 4 separate calls)")
        println("   - All 4 user IDs batched together: [user-alice, user-bob, user-charlie, user-diana]")
    }
}

