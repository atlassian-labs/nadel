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
    graphql.nadel.tests.next.update<`two deep renames`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `two deep renames snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   issues {
                |     __typename__deep_rename__authorId: __typename
                |     __typename__deep_rename__authorName: __typename
                |     deep_rename__authorId__authorDetails: authorDetails {
                |       authorId
                |     }
                |     deep_rename__authorName__authorDetails: authorDetails {
                |       name
                |     }
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "deep_rename__authorId__authorDetails": {
                |           "authorId": "USER-1"
                |         },
                |         "__typename__deep_rename__authorId": "Issue",
                |         "id": "ISSUE-1",
                |         "__typename__deep_rename__authorName": "Issue",
                |         "deep_rename__authorName__authorDetails": {
                |           "name": "User 1"
                |         }
                |       },
                |       {
                |         "deep_rename__authorId__authorDetails": {
                |           "authorId": "USER-2"
                |         },
                |         "__typename__deep_rename__authorId": "Issue",
                |         "id": "ISSUE-2",
                |         "__typename__deep_rename__authorName": "Issue",
                |         "deep_rename__authorName__authorDetails": {
                |           "name": "User 2"
                |         }
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
     *         "id": "ISSUE-1",
     *         "authorId": "USER-1",
     *         "authorName": "User 1"
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authorId": "USER-2",
     *         "authorName": "User 2"
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
            |         "id": "ISSUE-1",
            |         "authorId": "USER-1",
            |         "authorName": "User 1"
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authorId": "USER-2",
            |         "authorName": "User 2"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
