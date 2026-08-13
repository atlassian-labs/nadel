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
    graphql.nadel.tests.next.update<BatchHydrationCoalescingOverlappingListInputsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingOverlappingListInputsTestSnapshot : TestSnapshot() {
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
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/2", "ari:cloud:identity::user/3", "ari:cloud:identity::user/1"]) {
                |     batch_hydration_shared_0_0__approvers__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__0_0": [
                |       {
                |         "name": "Two",
                |         "batch_hydration_shared_0_0__approvers__id": "ari:cloud:identity::user/2"
                |       },
                |       {
                |         "name": "Three",
                |         "batch_hydration_shared_0_0__approvers__id": "ari:cloud:identity::user/3"
                |       },
                |       {
                |         "name": "One",
                |         "batch_hydration_shared_0_0__approvers__id": "ari:cloud:identity::user/1"
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
                |     batch_hydration__approvers__approverIds: approverIds
                |     batch_hydration__reviewers__reviewerIds: reviewerIds
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "batch_hydration__reviewers__reviewerIds": [
                |         "ari:cloud:identity::user/1",
                |         "ari:cloud:identity::user/2",
                |         "ari:cloud:identity::user/1"
                |       ],
                |       "__typename__batch_hydration__reviewers": "Issue",
                |       "batch_hydration__approvers__approverIds": [
                |         "ari:cloud:identity::user/2",
                |         "ari:cloud:identity::user/3",
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
     *           "name": "Two"
     *         },
     *         {
     *           "name": "Three"
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
     *         },
     *         {
     *           "name": "One"
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
            |           "name": "Two"
            |         },
            |         {
            |           "name": "Three"
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
            |         },
            |         {
            |           "name": "One"
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
