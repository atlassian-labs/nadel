// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`expecting one child error on extensive field argument passed to hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `expecting one child error on extensive field argument passed to hydration snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "TestBoard",
                query = """
                | {
                |   board(id: 1) {
                |     __typename__rename__cardChildren: __typename
                |     id
                |     rename__cardChildren__issueChildren: issueChildren {
                |       __typename__batch_hydration__assignee: __typename
                |       id
                |       batch_hydration__assignee__issue: issue {
                |         assignee {
                |           accountId
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
                |     "board": {
                |       "id": "1",
                |       "rename__cardChildren__issueChildren": [
                |         {
                |           "id": "a1",
                |           "batch_hydration__assignee__issue": {
                |             "assignee": {
                |               "accountId": "1"
                |             }
                |           },
                |           "__typename__batch_hydration__assignee": "Card"
                |         },
                |         {
                |           "id": "a2",
                |           "batch_hydration__assignee__issue": {
                |             "assignee": {
                |               "accountId": "2"
                |             }
                |           },
                |           "__typename__batch_hydration__assignee": "Card"
                |         },
                |         {
                |           "id": "a3",
                |           "batch_hydration__assignee__issue": {
                |             "assignee": {
                |               "accountId": "3"
                |             }
                |           },
                |           "__typename__batch_hydration__assignee": "Card"
                |         }
                |       ],
                |       "__typename__rename__cardChildren": "Board"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Users",
                query = """
                | {
                |   users(accountIds: ["1", "2", "3"]) {
                |     accountId
                |     batch_hydration__assignee__accountId: accountId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "accountId": "1",
                |         "batch_hydration__assignee__accountId": "1"
                |       },
                |       {
                |         "accountId": "2",
                |         "batch_hydration__assignee__accountId": "2"
                |       },
                |       {
                |         "accountId": "3",
                |         "batch_hydration__assignee__accountId": "3"
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
     *     "board": {
     *       "id": "1",
     *       "cardChildren": [
     *         {
     *           "id": "a1",
     *           "assignee": {
     *             "accountId": "1"
     *           }
     *         },
     *         {
     *           "id": "a2",
     *           "assignee": {
     *             "accountId": "2"
     *           }
     *         },
     *         {
     *           "id": "a3",
     *           "assignee": {
     *             "accountId": "3"
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
            |     "board": {
            |       "id": "1",
            |       "cardChildren": [
            |         {
            |           "id": "a1",
            |           "assignee": {
            |             "accountId": "1"
            |           }
            |         },
            |         {
            |           "id": "a2",
            |           "assignee": {
            |             "accountId": "2"
            |           }
            |         },
            |         {
            |           "id": "a3",
            |           "assignee": {
            |             "accountId": "3"
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
