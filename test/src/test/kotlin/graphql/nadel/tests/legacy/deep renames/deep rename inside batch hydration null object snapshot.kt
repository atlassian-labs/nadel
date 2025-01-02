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
    graphql.nadel.tests.next.update<`deep rename inside batch hydration null object`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class `deep rename inside batch hydration null object snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | {
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
                |         "deep_rename__name__detail": {
                |           "detailName": "Memes are the DNA of the soul"
                |         },
                |         "__typename__deep_rename__name": "Issue",
                |         "batch_hydration__issue__id": "issue-1"
                |       },
                |       null,
                |       {
                |         "deep_rename__name__detail": {
                |           "detailName": "Names are arbitrary"
                |         },
                |         "__typename__deep_rename__name": "Issue",
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
     *           "name": "Memes are the DNA of the soul"
     *         }
     *       },
     *       {
     *         "issue": null
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
            |           "name": "Memes are the DNA of the soul"
            |         }
            |       },
            |       {
            |         "issue": null
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
