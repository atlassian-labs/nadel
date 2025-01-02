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
    graphql.nadel.tests.next.update<`two deep renames merged fields with same path and field rename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `two deep renames merged fields with same path and field rename snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   issue {
                |     __typename__deep_rename__authorId: __typename
                |     __typename__deep_rename__authorName: __typename
                |     __typename__rename__details: __typename
                |     deep_rename__authorId__authorDetails: authorDetails {
                |       authorId
                |     }
                |     deep_rename__authorName__authorDetails: authorDetails {
                |       name
                |     }
                |     rename__details__authorDetails: authorDetails {
                |       __typename__rename__extra: __typename
                |       rename__extra__extraInfo: extraInfo
                |     }
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "deep_rename__authorId__authorDetails": {
                |         "authorId": "USER-1"
                |       },
                |       "__typename__deep_rename__authorId": "Issue",
                |       "__typename__rename__details": "Issue",
                |       "__typename__deep_rename__authorName": "Issue",
                |       "id": "ISSUE-1",
                |       "rename__details__authorDetails": {
                |         "__typename__rename__extra": "AuthorDetail",
                |         "rename__extra__extraInfo": "extra 1"
                |       },
                |       "deep_rename__authorName__authorDetails": {
                |         "name": "User 1"
                |       }
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
     *     "issue": {
     *       "id": "ISSUE-1",
     *       "authorId": "USER-1",
     *       "authorName": "User 1",
     *       "details": {
     *         "extra": "extra 1"
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
            |     "issue": {
            |       "id": "ISSUE-1",
            |       "authorId": "USER-1",
            |       "authorName": "User 1",
            |       "details": {
            |         "extra": "extra 1"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
