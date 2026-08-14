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
    graphql.nadel.tests.next.update<BatchHydrationCoalescingAliasedConsumersTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingAliasedConsumersTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issue {
     *     primaryUser: assignee {
     *       name
     *     }
     *     backupUser: assignee {
     *       name
     *     }
     *     reportingUser: reporter {
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
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/1", "ari:cloud:identity::user/2"]) {
                |     batch_hydration_shared_0_0__primaryUser__id: id
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
                |         "batch_hydration_shared_0_0__primaryUser__id": "ari:cloud:identity::user/1"
                |       },
                |       {
                |         "name": "Two",
                |         "batch_hydration_shared_0_0__primaryUser__id": "ari:cloud:identity::user/2"
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
                |     __typename__batch_hydration__primaryUser: __typename
                |     __typename__batch_hydration__backupUser: __typename
                |     __typename__batch_hydration__reportingUser: __typename
                |     batch_hydration__primaryUser__assigneeId: assigneeId
                |     batch_hydration__backupUser__assigneeId: assigneeId
                |     batch_hydration__reportingUser__reporterId: reporterId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "batch_hydration__primaryUser__assigneeId": "ari:cloud:identity::user/1",
                |       "__typename__batch_hydration__primaryUser": "Issue",
                |       "batch_hydration__backupUser__assigneeId": "ari:cloud:identity::user/1",
                |       "__typename__batch_hydration__backupUser": "Issue",
                |       "batch_hydration__reportingUser__reporterId": "ari:cloud:identity::user/2",
                |       "__typename__batch_hydration__reportingUser": "Issue"
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
     *       "primaryUser": {
     *         "name": "One"
     *       },
     *       "backupUser": {
     *         "name": "One"
     *       },
     *       "reportingUser": {
     *         "name": "Two"
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
            |     "issue": {
            |       "primaryUser": {
            |         "name": "One"
            |       },
            |       "backupUser": {
            |         "name": "One"
            |       },
            |       "reportingUser": {
            |         "name": "Two"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
