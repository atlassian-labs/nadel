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
    graphql.nadel.tests.next.update<`deep rename inside another rename of type List`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename inside another rename of type List snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   boardScope {
                |     board {
                |       __typename__rename__cardChildren: __typename
                |       rename__cardChildren__issueChildren: issueChildren {
                |         __typename__deep_rename__key: __typename
                |         __typename__deep_rename__summary: __typename
                |         id
                |         deep_rename__key__issue: issue {
                |           key
                |         }
                |         deep_rename__summary__issue: issue {
                |           summary
                |         }
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "boardScope": {
                |       "board": {
                |         "rename__cardChildren__issueChildren": [
                |           {
                |             "id": "1234",
                |             "deep_rename__key__issue": {
                |               "key": "abc"
                |             },
                |             "__typename__deep_rename__key": "Card",
                |             "deep_rename__summary__issue": {
                |               "summary": "Summary 1"
                |             },
                |             "__typename__deep_rename__summary": "Card"
                |           },
                |           {
                |             "id": "456",
                |             "deep_rename__key__issue": {
                |               "key": "def"
                |             },
                |             "__typename__deep_rename__key": "Card",
                |             "deep_rename__summary__issue": {
                |               "summary": "Summary 2"
                |             },
                |             "__typename__deep_rename__summary": "Card"
                |           }
                |         ],
                |         "__typename__rename__cardChildren": "Board"
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
     *     "boardScope": {
     *       "board": {
     *         "cardChildren": [
     *           {
     *             "id": "1234",
     *             "key": "abc",
     *             "summary": "Summary 1"
     *           },
     *           {
     *             "id": "456",
     *             "key": "def",
     *             "summary": "Summary 2"
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
            |     "boardScope": {
            |       "board": {
            |         "cardChildren": [
            |           {
            |             "id": "1234",
            |             "key": "abc",
            |             "summary": "Summary 1"
            |           },
            |           {
            |             "id": "456",
            |             "key": "def",
            |             "summary": "Summary 2"
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
