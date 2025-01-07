// @formatter:off
package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`renamed type inside batch hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed type inside batch hydration snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | {
                |   issuesByIds(id: ["issue-1", "issue-2", "issue-3"]) {
                |     details {
                |       __typename
                |       name
                |     }
                |     batch_hydration__issue__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issuesByIds": [
                |       {
                |         "details": {
                |           "__typename": "Details",
                |           "name": "Details of issue one"
                |         },
                |         "batch_hydration__issue__id": "issue-1"
                |       },
                |       {
                |         "details": {
                |           "__typename": "Details",
                |           "name": "Issue two"
                |         },
                |         "batch_hydration__issue__id": "issue-2"
                |       },
                |       {
                |         "details": {
                |           "__typename": "Details",
                |           "name": "Issue four – no wait three"
                |         },
                |         "batch_hydration__issue__id": "issue-3"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "UserService",
                query = """
                | {
                |   users {
                |     __typename__batch_hydration__issue: __typename
                |     batch_hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "batch_hydration__issue__issueId": "issue-1",
                |         "__typename__batch_hydration__issue": "User"
                |       },
                |       {
                |         "batch_hydration__issue__issueId": "issue-2",
                |         "__typename__batch_hydration__issue": "User"
                |       },
                |       {
                |         "batch_hydration__issue__issueId": "issue-3",
                |         "__typename__batch_hydration__issue": "User"
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
     *     "users": [
     *       {
     *         "issue": {
     *           "details": {
     *             "__typename": "IssueDetails",
     *             "name": "Details of issue one"
     *           }
     *         }
     *       },
     *       {
     *         "issue": {
     *           "details": {
     *             "__typename": "IssueDetails",
     *             "name": "Issue two"
     *           }
     *         }
     *       },
     *       {
     *         "issue": {
     *           "details": {
     *             "__typename": "IssueDetails",
     *             "name": "Issue four – no wait three"
     *           }
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
            |     "users": [
            |       {
            |         "issue": {
            |           "details": {
            |             "__typename": "IssueDetails",
            |             "name": "Details of issue one"
            |           }
            |         }
            |       },
            |       {
            |         "issue": {
            |           "details": {
            |             "__typename": "IssueDetails",
            |             "name": "Issue two"
            |           }
            |         }
            |       },
            |       {
            |         "issue": {
            |           "details": {
            |             "__typename": "IssueDetails",
            |             "name": "Issue four – no wait three"
            |           }
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
