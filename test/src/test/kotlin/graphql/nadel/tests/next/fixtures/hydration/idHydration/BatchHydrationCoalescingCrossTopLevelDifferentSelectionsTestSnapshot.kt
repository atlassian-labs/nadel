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
    graphql.nadel.tests.next.update<BatchHydrationCoalescingCrossTopLevelDifferentSelectionsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingCrossTopLevelDifferentSelectionsTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issue {
     *     assignee {
     *       displayName
     *     }
     *   }
     *   page {
     *     owner {
     *       email
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
                service = "Confluence",
                query = """
                | {
                |   page {
                |     __typename__batch_hydration__owner: __typename
                |     batch_hydration__owner__ownerId: ownerId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "page": {
                |       "batch_hydration__owner__ownerId": "ari:cloud:identity::user/confluence",
                |       "__typename__batch_hydration__owner": "Page"
                |     }
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
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/jira"]) {
                |     displayName
                |     batch_hydration_shared_0_0__assignee__id: id
                |   }
                |   batch_hydration__0_1: usersByIds(ids: ["ari:cloud:identity::user/confluence"]) {
                |     email
                |     batch_hydration_shared_0_1__owner__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__0_0": [
                |       {
                |         "displayName": "Jira User",
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/jira"
                |       }
                |     ],
                |     "batch_hydration__0_1": [
                |       {
                |         "email": "confluence@example.com",
                |         "batch_hydration_shared_0_1__owner__id": "ari:cloud:identity::user/confluence"
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
                |     __typename__batch_hydration__assignee: __typename
                |     batch_hydration__assignee__assigneeId: assigneeId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "batch_hydration__assignee__assigneeId": "ari:cloud:identity::user/jira",
                |       "__typename__batch_hydration__assignee": "Issue"
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
     *       "assignee": {
     *         "displayName": "Jira User"
     *       }
     *     },
     *     "page": {
     *       "owner": {
     *         "email": "confluence@example.com"
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
            |       "assignee": {
            |         "displayName": "Jira User"
            |       }
            |     },
            |     "page": {
            |       "owner": {
            |         "email": "confluence@example.com"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
