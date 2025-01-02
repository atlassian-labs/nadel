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
    graphql.nadel.tests.next.update<`deep rename nested inside renamed type`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename nested inside renamed type snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   first {
                |     user {
                |       __typename__deep_rename__name: __typename
                |       deep_rename__name__details: details {
                |         firstName
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "first": {
                |       "user": {
                |         "__typename__deep_rename__name": "User",
                |         "deep_rename__name__details": {
                |           "firstName": "name-from-details"
                |         }
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   second: first {
                |     __typename
                |     user {
                |       __typename__deep_rename__name: __typename
                |       deep_rename__name__details: details {
                |         firstName
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "second": {
                |       "__typename": "Issue",
                |       "user": {
                |         "__typename__deep_rename__name": "User",
                |         "deep_rename__name__details": {
                |           "firstName": "name-from-details-2"
                |         }
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
     *     "first": {
     *       "user": {
     *         "name": "name-from-details"
     *       }
     *     },
     *     "second": {
     *       "__typename": "JiraIssue",
     *       "user": {
     *         "name": "name-from-details-2"
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
            |     "first": {
            |       "user": {
            |         "name": "name-from-details"
            |       }
            |     },
            |     "second": {
            |       "__typename": "JiraIssue",
            |       "user": {
            |         "name": "name-from-details-2"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
