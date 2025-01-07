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
    graphql.nadel.tests.next.update<`renamed type inside deep rename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed type inside deep rename snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | {
                |   issueById(id: "issue-1") {
                |     __typename__deep_rename__assignee: __typename
                |     deep_rename__assignee__details: details {
                |       assignee {
                |         __typename
                |         friends {
                |           __typename
                |         }
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "deep_rename__assignee__details": {
                |         "assignee": {
                |           "name": "Franklin",
                |           "__typename": "User",
                |           "friends": [
                |             {
                |               "__typename": "User"
                |             },
                |             {
                |               "__typename": "User"
                |             }
                |           ]
                |         }
                |       },
                |       "__typename__deep_rename__assignee": "Issue"
                |     }
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
     *     "issueById": {
     *       "assignee": {
     *         "name": "Franklin",
     *         "__typename": "IssueUser",
     *         "friends": [
     *           {
     *             "__typename": "IssueUser"
     *           },
     *           {
     *             "__typename": "IssueUser"
     *           }
     *         ]
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issueById": {
            |       "assignee": {
            |         "name": "Franklin",
            |         "__typename": "IssueUser",
            |         "friends": [
            |           {
            |             "__typename": "IssueUser"
            |           },
            |           {
            |             "__typename": "IssueUser"
            |           }
            |         ]
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
