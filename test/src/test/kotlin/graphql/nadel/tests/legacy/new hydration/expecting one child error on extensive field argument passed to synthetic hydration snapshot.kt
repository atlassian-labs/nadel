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
    graphql.nadel.tests.next.update<`expecting one child error on extensive field argument passed to synthetic hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `expecting one child error on extensive field argument passed to synthetic hydration snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "TestBoard",
                query = """
                | query {
                |   board(id: 1) {
                |     __typename__rename__cardChildren: __typename
                |     id
                |     rename__cardChildren__issueChildren: issueChildren {
                |       __typename__batch_hydration__assignee: __typename
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
                |       "__typename__rename__cardChildren": "Board",
                |       "id": "1",
                |       "rename__cardChildren__issueChildren": [
                |         {
                |           "__typename__batch_hydration__assignee": "Card",
                |           "batch_hydration__assignee__issue": {
                |             "assignee": {
                |               "accountId": "1"
                |             }
                |           }
                |         },
                |         {
                |           "__typename__batch_hydration__assignee": "Card",
                |           "batch_hydration__assignee__issue": {
                |             "assignee": {
                |               "accountId": "2"
                |             }
                |           }
                |         },
                |         {
                |           "__typename__batch_hydration__assignee": "Card",
                |           "batch_hydration__assignee__issue": {
                |             "assignee": {
                |               "accountId": "3"
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
            ),
            ExpectedServiceCall(
                service = "Users",
                query = """
                | query {
                |   usersQuery {
                |     users(accountIds: ["1", "2", "3"]) {
                |       accountId
                |       batch_hydration__assignee__accountId: accountId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersQuery": {
                |       "users": [
                |         {
                |           "batch_hydration__assignee__accountId": "1",
                |           "accountId": "1"
                |         },
                |         {
                |           "batch_hydration__assignee__accountId": "2",
                |           "accountId": "2"
                |         },
                |         {
                |           "batch_hydration__assignee__accountId": "3",
                |           "accountId": "3"
                |         }
                |       ]
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
     *     "board": {
     *       "id": "1",
     *       "cardChildren": [
     *         {
     *           "assignee": {
     *             "accountId": "1"
     *           }
     *         },
     *         {
     *           "assignee": {
     *             "accountId": "2"
     *           }
     *         },
     *         {
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
            |           "assignee": {
            |             "accountId": "1"
            |           }
            |         },
            |         {
            |           "assignee": {
            |             "accountId": "2"
            |           }
            |         },
            |         {
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
