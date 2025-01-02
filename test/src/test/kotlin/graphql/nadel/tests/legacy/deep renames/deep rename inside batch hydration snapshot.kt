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
    graphql.nadel.tests.next.update<`deep rename inside batch hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename inside batch hydration snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   issuesByIds(id: ["issue-1", "issue-2", "issue-3"]) {
                |     __typename__deep_rename__name: __typename
                |     deep_rename__name__detail: detail {
                |       detailName
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
                |         "batch_hydration__issue__id": "issue-1",
                |         "deep_rename__name__detail": {
                |           "detailName": "It amounts to nothing"
                |         },
                |         "__typename__deep_rename__name": "Issue"
                |       },
                |       {
                |         "batch_hydration__issue__id": "issue-2",
                |         "deep_rename__name__detail": {
                |           "detailName": "Details are cool"
                |         },
                |         "__typename__deep_rename__name": "Issue"
                |       },
                |       {
                |         "batch_hydration__issue__id": "issue-3",
                |         "deep_rename__name__detail": {
                |           "detailName": "Names are arbitrary"
                |         },
                |         "__typename__deep_rename__name": "Issue"
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
                | query {
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
                |         "__typename__batch_hydration__issue": "User",
                |         "batch_hydration__issue__issueId": "issue-1"
                |       },
                |       {
                |         "__typename__batch_hydration__issue": "User",
                |         "batch_hydration__issue__issueId": "issue-2"
                |       },
                |       {
                |         "__typename__batch_hydration__issue": "User",
                |         "batch_hydration__issue__issueId": "issue-3"
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
     *           "name": "It amounts to nothing"
     *         }
     *       },
     *       {
     *         "issue": {
     *           "name": "Details are cool"
     *         }
     *       },
     *       {
     *         "issue": {
     *           "name": "Names are arbitrary"
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
            |           "name": "It amounts to nothing"
            |         }
            |       },
            |       {
            |         "issue": {
            |           "name": "Details are cool"
            |         }
            |       },
            |       {
            |         "issue": {
            |           "name": "Names are arbitrary"
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
