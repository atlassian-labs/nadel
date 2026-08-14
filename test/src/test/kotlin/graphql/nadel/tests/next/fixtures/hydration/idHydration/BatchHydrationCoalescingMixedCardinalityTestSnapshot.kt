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
    graphql.nadel.tests.next.update<BatchHydrationCoalescingMixedCardinalityTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingMixedCardinalityTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issue {
     *     primary {
     *       name
     *     }
     *     summary
     *     reviewers {
     *       name
     *     }
     *     absent {
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
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/1", "ari:cloud:identity::user/3"]) {
                |     batch_hydration_shared_0_0__primary__id: id
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
                |         "name": "One",
                |         "batch_hydration_shared_0_0__primary__id": "ari:cloud:identity::user/1"
                |       },
                |       {
                |         "name": "Three",
                |         "batch_hydration_shared_0_0__primary__id": "ari:cloud:identity::user/3"
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
                |   batch_hydration__0_1: usersByIds(ids: ["ari:cloud:identity::user/2"]) {
                |     batch_hydration_shared_0_0__primary__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__0_1": [
                |       {
                |         "name": "Two",
                |         "batch_hydration_shared_0_0__primary__id": "ari:cloud:identity::user/2"
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
                |     __typename__batch_hydration__primary: __typename
                |     __typename__batch_hydration__reviewers: __typename
                |     __typename__batch_hydration__absent: __typename
                |     batch_hydration__absent__absentId: absentId
                |     batch_hydration__primary__primaryId: primaryId
                |     batch_hydration__reviewers__reviewerIds: reviewerIds
                |     summary
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "batch_hydration__primary__primaryId": "ari:cloud:identity::user/1",
                |       "__typename__batch_hydration__primary": "Issue",
                |       "summary": "Between hydrated fields",
                |       "batch_hydration__reviewers__reviewerIds": [
                |         "ari:cloud:identity::user/3",
                |         "ari:cloud:identity::user/1",
                |         "ari:cloud:identity::user/3",
                |         "ari:cloud:identity::user/2"
                |       ],
                |       "__typename__batch_hydration__reviewers": "Issue",
                |       "batch_hydration__absent__absentId": null,
                |       "__typename__batch_hydration__absent": "Issue"
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
     *       "primary": {
     *         "name": "One"
     *       },
     *       "summary": "Between hydrated fields",
     *       "reviewers": [
     *         {
     *           "name": "Three"
     *         },
     *         {
     *           "name": "One"
     *         },
     *         {
     *           "name": "Three"
     *         },
     *         {
     *           "name": "Two"
     *         }
     *       ],
     *       "absent": null
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
            |       "primary": {
            |         "name": "One"
            |       },
            |       "summary": "Between hydrated fields",
            |       "reviewers": [
            |         {
            |           "name": "Three"
            |         },
            |         {
            |           "name": "One"
            |         },
            |         {
            |           "name": "Three"
            |         },
            |         {
            |           "name": "Two"
            |         }
            |       ],
            |       "absent": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
