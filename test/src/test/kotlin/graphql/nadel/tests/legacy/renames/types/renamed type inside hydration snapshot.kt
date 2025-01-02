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
    graphql.nadel.tests.next.update<`renamed type inside hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed type inside hydration snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | {
                |   issueById(id: "issue-2") {
                |     details {
                |       __typename
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "details": {
                |         "__typename": "Details",
                |         "name": "Issue two"
                |       }
                |     }
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
                |   me {
                |     __typename__hydration__issue: __typename
                |     hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "me": {
                |       "hydration__issue__issueId": "issue-2",
                |       "__typename__hydration__issue": "User"
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
     *     "me": {
     *       "issue": {
     *         "details": {
     *           "__typename": "IssueDetails",
     *           "name": "Issue two"
     *         }
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
            |     "me": {
            |       "issue": {
            |         "details": {
            |           "__typename": "IssueDetails",
            |           "name": "Issue two"
            |         }
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
