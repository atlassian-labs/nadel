// @formatter:off
package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`a lot of renames`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `a lot of renames snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Boards",
                query = """
                | {
                |   boardScope {
                |     __typename__rename__cardParents: __typename
                |     rename__cardParents__issueParents: issueParents {
                |       __typename__rename__cardType: __typename
                |       rename__cardType__issueType: issueType {
                |         __typename__rename__inlineCardCreate: __typename
                |         id
                |         rename__inlineCardCreate__inlineIssueCreate: inlineIssueCreate {
                |           enabled
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
                |       "rename__cardParents__issueParents": [
                |         {
                |           "rename__cardType__issueType": {
                |             "id": "ID-1",
                |             "rename__inlineCardCreate__inlineIssueCreate": {
                |               "enabled": true
                |             },
                |             "__typename__rename__inlineCardCreate": "IssueType"
                |           },
                |           "__typename__rename__cardType": "IssueParent"
                |         }
                |       ],
                |       "__typename__rename__cardParents": "BoardScope"
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
     *       "cardParents": [
     *         {
     *           "cardType": {
     *             "id": "ID-1",
     *             "inlineCardCreate": {
     *               "enabled": true
     *             }
     *           }
     *         }
     *       ]
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
            |       "cardParents": [
            |         {
            |           "cardType": {
            |             "id": "ID-1",
            |             "inlineCardCreate": {
            |               "enabled": true
            |             }
            |           }
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
