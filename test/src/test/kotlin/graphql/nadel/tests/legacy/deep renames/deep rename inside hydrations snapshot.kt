// @formatter:off
package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`deep rename inside hydrations`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename inside hydrations snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   issueById(id: "issue-1") {
                |     __typename__deep_rename__name: __typename
                |     deep_rename__name__detail: detail {
                |       detailName
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "deep_rename__name__detail": {
                |         "detailName": "Detail-1"
                |       },
                |       "__typename__deep_rename__name": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   issueById(id: "issue-2") {
                |     __typename__deep_rename__name: __typename
                |     deep_rename__name__detail: detail {
                |       detailName
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "deep_rename__name__detail": {
                |         "detailName": "Detail-2"
                |       },
                |       "__typename__deep_rename__name": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   issueById(id: "issue-3") {
                |     __typename__deep_rename__name: __typename
                |     deep_rename__name__detail: detail {
                |       detailName
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "deep_rename__name__detail": {
                |         "detailName": "A name goes here"
                |       },
                |       "__typename__deep_rename__name": "Issue"
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
                | query {
                |   users {
                |     __typename__hydration__issue: __typename
                |     hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "__typename__hydration__issue": "User",
                |         "hydration__issue__issueId": "issue-1"
                |       },
                |       {
                |         "__typename__hydration__issue": "User",
                |         "hydration__issue__issueId": "issue-2"
                |       },
                |       {
                |         "__typename__hydration__issue": "User",
                |         "hydration__issue__issueId": "issue-3"
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
     *           "name": "Detail-1"
     *         }
     *       },
     *       {
     *         "issue": {
     *           "name": "Detail-2"
     *         }
     *       },
     *       {
     *         "issue": {
     *           "name": "A name goes here"
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
            |           "name": "Detail-1"
            |         }
            |       },
            |       {
            |         "issue": {
            |           "name": "Detail-2"
            |         }
            |       },
            |       {
            |         "issue": {
            |           "name": "A name goes here"
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
