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
    graphql.nadel.tests.next.update<BatchHydrationCoalescingCrossTopLevelServicesTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingCrossTopLevelServicesTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issue {
     *     assignee {
     *       name
     *       displayName
     *     }
     *   }
     *   page {
     *     owner {
     *       name
     *       displayName
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
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/jira", "ari:cloud:identity::user/confluence"]) {
                |     displayName
                |     batch_hydration_shared_0_0__assignee__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Name unavailable",
                |       "path": [
                |         "batch_hydration__0_0",
                |         0,
                |         "name"
                |       ],
                |       "extensions": {
                |         "classification": "NameUnavailableError"
                |       }
                |     },
                |     {
                |       "message": "Name unavailable",
                |       "path": [
                |         "batch_hydration__0_0",
                |         1,
                |         "name"
                |       ],
                |       "extensions": {
                |         "classification": "NameUnavailableError"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "batch_hydration__0_0": [
                |       {
                |         "name": null,
                |         "displayName": "Jira User",
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/jira"
                |       },
                |       {
                |         "name": null,
                |         "displayName": "Confluence User",
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/confluence"
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
     *   "errors": [
     *     {
     *       "message": "Name unavailable",
     *       "locations": [],
     *       "path": [
     *         "issue",
     *         "assignee",
     *         "name"
     *       ],
     *       "extensions": {
     *         "classification": "NameUnavailableError"
     *       }
     *     },
     *     {
     *       "message": "Name unavailable",
     *       "locations": [],
     *       "path": [
     *         "page",
     *         "owner",
     *         "name"
     *       ],
     *       "extensions": {
     *         "classification": "NameUnavailableError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "issue": {
     *       "assignee": {
     *         "name": null,
     *         "displayName": "Jira User"
     *       }
     *     },
     *     "page": {
     *       "owner": {
     *         "name": null,
     *         "displayName": "Confluence User"
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Name unavailable",
            |       "locations": [],
            |       "path": [
            |         "issue",
            |         "assignee",
            |         "name"
            |       ],
            |       "extensions": {
            |         "classification": "NameUnavailableError"
            |       }
            |     },
            |     {
            |       "message": "Name unavailable",
            |       "locations": [],
            |       "path": [
            |         "page",
            |         "owner",
            |         "name"
            |       ],
            |       "extensions": {
            |         "classification": "NameUnavailableError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "issue": {
            |       "assignee": {
            |         "name": null,
            |         "displayName": "Jira User"
            |       }
            |     },
            |     "page": {
            |       "owner": {
            |         "name": null,
            |         "displayName": "Confluence User"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
