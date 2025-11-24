// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchHydrationWithConnectionEdgesTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationWithConnectionEdgesTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issueConnection {
                |     edges {
                |       cursor
                |       node {
                |         id
                |         key
                |         batch_hydration__assignee__assigneeId: assigneeId
                |         __typename__batch_hydration__assignee: __typename
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueConnection": {
                |       "edges": [
                |         {
                |           "cursor": "cursor-issue-1",
                |           "node": {
                |             "id": "issue-1",
                |             "key": "PROJ-1",
                |             "batch_hydration__assignee__assigneeId": "user-alice",
                |             "__typename__batch_hydration__assignee": "Issue"
                |           }
                |         },
                |         {
                |           "cursor": "cursor-issue-2",
                |           "node": {
                |             "id": "issue-2",
                |             "key": "PROJ-2",
                |             "batch_hydration__assignee__assigneeId": "user-bob",
                |             "__typename__batch_hydration__assignee": "Issue"
                |           }
                |         },
                |         {
                |           "cursor": "cursor-issue-3",
                |           "node": {
                |             "id": "issue-3",
                |             "key": "PROJ-3",
                |             "batch_hydration__assignee__assigneeId": "user-charlie",
                |             "__typename__batch_hydration__assignee": "Issue"
                |           }
                |         },
                |         {
                |           "cursor": "cursor-issue-4",
                |           "node": {
                |             "id": "issue-4",
                |             "key": "PROJ-4",
                |             "batch_hydration__assignee__assigneeId": "user-diana",
                |             "__typename__batch_hydration__assignee": "Issue"
                |           }
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "identity",
                query = """
                | {
                |   users(accountIds: ["user-alice", "user-bob", "user-charlie", "user-diana"]) {
                |     id
                |     name
                |     batch_hydration__assignee__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "id": "user-alice",
                |         "name": "Alice",
                |         "batch_hydration__assignee__id": "user-alice"
                |       },
                |       {
                |         "id": "user-bob",
                |         "name": "Bob",
                |         "batch_hydration__assignee__id": "user-bob"
                |       },
                |       {
                |         "id": "user-charlie",
                |         "name": "Charlie",
                |         "batch_hydration__assignee__id": "user-charlie"
                |       },
                |       {
                |         "id": "user-diana",
                |         "name": "Diana",
                |         "batch_hydration__assignee__id": "user-diana"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "issueConnection": {
     *       "edges": [
     *         {
     *           "node": {
     *             "id": "issue-1",
     *             "key": "PROJ-1",
     *             "assignee": {
     *               "id": "user-alice",
     *               "name": "Alice"
     *             }
     *           },
     *           "cursor": "cursor-issue-1"
     *         },
     *         {
     *           "node": {
     *             "id": "issue-2",
     *             "key": "PROJ-2",
     *             "assignee": {
     *               "id": "user-bob",
     *               "name": "Bob"
     *             }
     *           },
     *           "cursor": "cursor-issue-2"
     *         },
     *         {
     *           "node": {
     *             "id": "issue-3",
     *             "key": "PROJ-3",
     *             "assignee": {
     *               "id": "user-charlie",
     *               "name": "Charlie"
     *             }
     *           },
     *           "cursor": "cursor-issue-3"
     *         },
     *         {
     *           "node": {
     *             "id": "issue-4",
     *             "key": "PROJ-4",
     *             "assignee": {
     *               "id": "user-diana",
     *               "name": "Diana"
     *             }
     *           },
     *           "cursor": "cursor-issue-4"
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issueConnection": {
            |       "edges": [
            |         {
            |           "node": {
            |             "id": "issue-1",
            |             "key": "PROJ-1",
            |             "assignee": {
            |               "id": "user-alice",
            |               "name": "Alice"
            |             }
            |           },
            |           "cursor": "cursor-issue-1"
            |         },
            |         {
            |           "node": {
            |             "id": "issue-2",
            |             "key": "PROJ-2",
            |             "assignee": {
            |               "id": "user-bob",
            |               "name": "Bob"
            |             }
            |           },
            |           "cursor": "cursor-issue-2"
            |         },
            |         {
            |           "node": {
            |             "id": "issue-3",
            |             "key": "PROJ-3",
            |             "assignee": {
            |               "id": "user-charlie",
            |               "name": "Charlie"
            |             }
            |           },
            |           "cursor": "cursor-issue-3"
            |         },
            |         {
            |           "node": {
            |             "id": "issue-4",
            |             "key": "PROJ-4",
            |             "assignee": {
            |               "id": "user-diana",
            |               "name": "Diana"
            |             }
            |           },
            |           "cursor": "cursor-issue-4"
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}

