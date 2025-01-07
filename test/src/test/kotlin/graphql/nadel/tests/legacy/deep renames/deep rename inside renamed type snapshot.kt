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
    graphql.nadel.tests.next.update<`deep rename inside renamed type`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename inside renamed type snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   first {
                |     __typename
                |     __typename__deep_rename__name: __typename
                |     deep_rename__name__details: details {
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "first": {
                |       "__typename": "Issue",
                |       "deep_rename__name__details": {
                |         "name": "name-from-details"
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
                service = "Issues",
                query = """
                | {
                |   second: first {
                |     __typename__deep_rename__name: __typename
                |     deep_rename__name__details: details {
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "second": {
                |       "deep_rename__name__details": {
                |         "name": "name-from-details-2"
                |       },
                |       "__typename__deep_rename__name": "Issue"
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
     *       "__typename": "JiraIssue",
     *       "name": "name-from-details"
     *     },
     *     "second": {
     *       "name": "name-from-details-2"
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
            |       "__typename": "JiraIssue",
            |       "name": "name-from-details"
            |     },
            |     "second": {
            |       "name": "name-from-details-2"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
