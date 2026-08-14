// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchHydrationCoalescingThreeSelectionLanesTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingThreeSelectionLanesTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issues {
     *     assignee {
     *       name
     *     }
     *     reporter {
     *       email
     *     }
     *     creator {
     *       avatarUrl
     *     }
     *   }
     * }
     * ```
     *
     * Variables
     *
     * ```json
     * {}
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Identity",
                query = """
                | {
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/1"]) {
                |     batch_hydration_shared_0_0__assignee__id: id
                |     name
                |   }
                |   batch_hydration__0_1: usersByIds(ids: ["ari:cloud:identity::user/2"]) {
                |     email
                |     batch_hydration_shared_0_1__reporter__id: id
                |   }
                |   batch_hydration__0_2: usersByIds(ids: ["ari:cloud:identity::user/3"]) {
                |     avatarUrl
                |     batch_hydration_shared_0_2__creator__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__0_0": [
                |       {
                |         "name": "User 1",
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/1"
                |       }
                |     ],
                |     "batch_hydration__0_1": [
                |       {
                |         "email": "user2@example.com",
                |         "batch_hydration_shared_0_1__reporter__id": "ari:cloud:identity::user/2"
                |       }
                |     ],
                |     "batch_hydration__0_2": [
                |       {
                |         "avatarUrl": "https://example.com/user-3.png",
                |         "batch_hydration_shared_0_2__creator__id": "ari:cloud:identity::user/3"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Jira",
                query = """
                | {
                |   issues {
                |     __typename__batch_hydration__assignee: __typename
                |     __typename__batch_hydration__reporter: __typename
                |     __typename__batch_hydration__creator: __typename
                |     batch_hydration__assignee__assigneeId: assigneeId
                |     batch_hydration__creator__creatorId: creatorId
                |     batch_hydration__reporter__reporterId: reporterId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__assignee": "Issue",
                |         "batch_hydration__reporter__reporterId": "ari:cloud:identity::user/2",
                |         "__typename__batch_hydration__reporter": "Issue",
                |         "batch_hydration__creator__creatorId": "ari:cloud:identity::user/3",
                |         "__typename__batch_hydration__creator": "Issue"
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
     *     "issues": [
     *       {
     *         "assignee": {
     *           "name": "User 1"
     *         },
     *         "reporter": {
     *           "email": "user2@example.com"
     *         },
     *         "creator": {
     *           "avatarUrl": "https://example.com/user-3.png"
     *         }
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issues": [
            |       {
            |         "assignee": {
            |           "name": "User 1"
            |         },
            |         "reporter": {
            |           "email": "user2@example.com"
            |         },
            |         "creator": {
            |           "avatarUrl": "https://example.com/user-3.png"
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
