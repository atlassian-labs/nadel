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
    graphql.nadel.tests.next.update<BatchHydrationCoalescingDifferentSelectionsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingDifferentSelectionsTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issues {
     *     assigneeUser: assignee {
     *       picture: avatar(size: 32)
     *     }
     *     reporterUser: reporter {
     *       picture: avatar(size: 64)
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
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/1"]) {
                |     picture: avatar(size: 32)
                |     batch_hydration_shared_0_0__assigneeUser__id: id
                |   }
                |   batch_hydration__0_1: usersByIds(ids: ["ari:cloud:identity::user/1"]) {
                |     picture: avatar(size: 64)
                |     batch_hydration_shared_0_1__reporterUser__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__0_0": [
                |       {
                |         "picture": "One-32",
                |         "batch_hydration_shared_0_0__assigneeUser__id": "ari:cloud:identity::user/1"
                |       }
                |     ],
                |     "batch_hydration__0_1": [
                |       {
                |         "picture": "One-64",
                |         "batch_hydration_shared_0_1__reporterUser__id": "ari:cloud:identity::user/1"
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
                |   issues {
                |     __typename__batch_hydration__assigneeUser: __typename
                |     __typename__batch_hydration__reporterUser: __typename
                |     batch_hydration__assigneeUser__assigneeId: assigneeId
                |     batch_hydration__reporterUser__reporterId: reporterId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__assigneeUser__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__assigneeUser": "Issue",
                |         "batch_hydration__reporterUser__reporterId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__reporterUser": "Issue"
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
     *     "issues": [
     *       {
     *         "reporterUser": {
     *           "picture": "One-64"
     *         },
     *         "assigneeUser": {
     *           "picture": "One-32"
     *         }
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
            |     "issues": [
            |       {
            |         "reporterUser": {
            |           "picture": "One-64"
            |         },
            |         "assigneeUser": {
            |           "picture": "One-32"
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
