// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchHydrationCoalescingIndexedIsolationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingIndexedIsolationTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issue {
     *     reviewers {
     *       name
     *     }
     *     approvers {
     *       name
     *     }
     *   }
     * }
     * ```
     *
     * Variables
     *
     * ```json
     * {}
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Identity",
                query = """
                | {
                |   usersByIds(ids: ["ari:cloud:identity::user/1", "ari:cloud:identity::user/2"]) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "name": "One"
                |       },
                |       {
                |         "name": "Two"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Identity",
                query = """
                | {
                |   usersByIds(ids: ["ari:cloud:identity::user/1", "ari:cloud:identity::user/2"]) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "name": "One"
                |       },
                |       {
                |         "name": "Two"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Jira",
                query = """
                | {
                |   issue {
                |     __typename__batch_hydration__reviewers: __typename
                |     __typename__batch_hydration__approvers: __typename
                |     batch_hydration__reviewers__userIds: userIds
                |     batch_hydration__approvers__userIds: userIds
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "batch_hydration__reviewers__userIds": [
                |         "ari:cloud:identity::user/1",
                |         "ari:cloud:identity::user/2"
                |       ],
                |       "__typename__batch_hydration__reviewers": "Issue",
                |       "batch_hydration__approvers__userIds": [
                |         "ari:cloud:identity::user/1",
                |         "ari:cloud:identity::user/2"
                |       ],
                |       "__typename__batch_hydration__approvers": "Issue"
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
     *       "approvers": [
     *         {
     *           "name": "One"
     *         },
     *         {
     *           "name": "Two"
     *         }
     *       ],
     *       "reviewers": [
     *         {
     *           "name": "One"
     *         },
     *         {
     *           "name": "Two"
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
            |     "issue": {
            |       "approvers": [
            |         {
            |           "name": "One"
            |         },
            |         {
            |           "name": "Two"
            |         }
            |       ],
            |       "reviewers": [
            |         {
            |           "name": "One"
            |         },
            |         {
            |           "name": "Two"
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
