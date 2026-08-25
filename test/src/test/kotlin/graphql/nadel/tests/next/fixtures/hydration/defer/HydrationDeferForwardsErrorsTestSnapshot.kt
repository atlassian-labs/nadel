// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationDeferForwardsErrorsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferForwardsErrorsTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | query (${'$'}v0: String!) {
                |   issueByKey(key: ${'$'}v0) {
                |     __typename__hydration__assignee: __typename
                |     hydration__assignee__assigneeId: assigneeId
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "GQLGW-2"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issueByKey": {
                |       "hydration__assignee__assigneeId": "ari:cloud:identity::user/0",
                |       "__typename__hydration__assignee": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | query (${'$'}v0: ID!) {
                |   userById(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ari:cloud:identity::user/0"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "No user: ari:cloud:identity::user/0",
                |       "extensions": {
                |         "classification": "UserNotFoundError"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "userById": null
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
     *     "issueByKey": {
     *       "assignee": null
     *     }
     *   },
     *   "errors": [
     *     {
     *       "message": "No user: ari:cloud:identity::user/0",
     *       "locations": [],
     *       "path": [
     *         "issueByKey",
     *         "assignee"
     *       ],
     *       "extensions": {
     *         "classification": "UserNotFoundError"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issueByKey": {}
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
                """
                | {
                |   "hasNext": false,
                |   "incremental": [
                |     {
                |       "path": [
                |         "issueByKey"
                |       ],
                |       "errors": [
                |         {
                |           "message": "No user: ari:cloud:identity::user/0",
                |           "locations": [],
                |           "path": [
                |             "issueByKey",
                |             "assignee"
                |           ],
                |           "extensions": {
                |             "classification": "UserNotFoundError"
                |           }
                |         }
                |       ],
                |       "data": {
                |         "assignee": null
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
