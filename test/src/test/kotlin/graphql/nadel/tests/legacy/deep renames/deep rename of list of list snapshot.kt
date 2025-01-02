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
    graphql.nadel.tests.next.update<`deep rename of list of list`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename of list of list snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   details {
                |     __typename__deep_rename__labels: __typename
                |     deep_rename__labels__issue: issue {
                |       labels
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "details": [
                |       {
                |         "deep_rename__labels__issue": {
                |           "labels": [
                |             [
                |               "label1",
                |               "label2"
                |             ],
                |             [
                |               "label3"
                |             ]
                |           ]
                |         },
                |         "__typename__deep_rename__labels": "IssueDetail"
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
     *     "details": [
     *       {
     *         "labels": [
     *           [
     *             "label1",
     *             "label2"
     *           ],
     *           [
     *             "label3"
     *           ]
     *         ]
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
            |     "details": [
            |       {
            |         "labels": [
            |           [
            |             "label1",
            |             "label2"
            |           ],
            |           [
            |             "label3"
            |           ]
            |         ]
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
