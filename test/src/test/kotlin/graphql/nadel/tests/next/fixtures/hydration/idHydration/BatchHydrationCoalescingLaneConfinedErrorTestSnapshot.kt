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
    graphql.nadel.tests.next.update<BatchHydrationCoalescingLaneConfinedErrorTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingLaneConfinedErrorTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issues {
     *     assignee {
     *       name
     *     }
     *     reporter {
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
                service = "Identity",
                query = """
                | {
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/1"]) {
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
                |     }
                |   ],
                |   "data": {
                |     "batch_hydration__0_0": [
                |       {
                |         "name": null,
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/1"
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
                |   batch_hydration__0_1: usersByIds(ids: ["ari:cloud:identity::user/1"]) {
                |     email
                |     batch_hydration_shared_0_1__reporter__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__0_1": [
                |       {
                |         "email": "one@example.com",
                |         "batch_hydration_shared_0_1__reporter__id": "ari:cloud:identity::user/1"
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
                |     __typename__batch_hydration__assignee: __typename
                |     __typename__batch_hydration__reporter: __typename
                |     batch_hydration__assignee__assigneeId: assigneeId
                |     batch_hydration__reporter__reporterId: reporterId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__assignee": "Issue",
                |         "batch_hydration__reporter__reporterId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__reporter": "Issue"
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
     *   "errors": [
     *     {
     *       "message": "Name unavailable",
     *       "locations": [],
     *       "path": [
     *         "issues",
     *         0,
     *         "assignee",
     *         "name"
     *       ],
     *       "extensions": {
     *         "classification": "NameUnavailableError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "issues": [
     *       {
     *         "reporter": {
     *           "email": "one@example.com"
     *         },
     *         "assignee": {
     *           "name": null
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
            |   "errors": [
            |     {
            |       "message": "Name unavailable",
            |       "locations": [],
            |       "path": [
            |         "issues",
            |         0,
            |         "assignee",
            |         "name"
            |       ],
            |       "extensions": {
            |         "classification": "NameUnavailableError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "issues": [
            |       {
            |         "reporter": {
            |           "email": "one@example.com"
            |         },
            |         "assignee": {
            |           "name": null
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
