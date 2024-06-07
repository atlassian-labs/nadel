// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferForwardsErrorsFromEachHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issuesByKeys(keys: ["GQLGW-2", "GQLGW-3", "GQLGW-4"]) {
                |     key
                |     hydration__assignee__assigneeId: assigneeId
                |     __typename__hydration__assignee: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issuesByKeys": [
                |       {
                |         "key": "GQLGW-2",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/0",
                |         "__typename__hydration__assignee": "Issue"
                |       },
                |       {
                |         "key": "GQLGW-3",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__hydration__assignee": "Issue"
                |       },
                |       {
                |         "key": "GQLGW-4",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/10",
                |         "__typename__hydration__assignee": "Issue"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | {
                |   userById(id: "ari:cloud:identity::user/0") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
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
            ExpectedServiceCall(
                service = "users",
                query = """
                | {
                |   userById(id: "ari:cloud:identity::user/1") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Frank"
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
                | {
                |   userById(id: "ari:cloud:identity::user/10") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "No user: ari:cloud:identity::user/10",
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
     *     "issuesByKeys": [
     *       {
     *         "key": "GQLGW-2",
     *         "assignee": null
     *       },
     *       {
     *         "key": "GQLGW-3",
     *         "assignee": {
     *           "name": "Frank"
     *         }
     *       },
     *       {
     *         "key": "GQLGW-4",
     *         "assignee": null
     *       }
     *     ]
     *   },
     *   "errors": [
     *     {
     *       "message": "No user: ari:cloud:identity::user/0",
     *       "locations": [],
     *       "path": [
     *         "issuesByKeys",
     *         0,
     *         "assignee"
     *       ],
     *       "extensions": {
     *         "classification": "UserNotFoundError"
     *       }
     *     },
     *     {
     *       "message": "No user: ari:cloud:identity::user/10",
     *       "locations": [],
     *       "path": [
     *         "issuesByKeys",
     *         2,
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
            |     "issuesByKeys": [
            |       {
            |         "key": "GQLGW-2"
            |       },
            |       {
            |         "key": "GQLGW-3"
            |       },
            |       {
            |         "key": "GQLGW-4"
            |       }
            |     ]
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
                |         "issuesByKeys",
                |         0
                |       ],
                |       "errors": [
                |         {
                |           "message": "No user: ari:cloud:identity::user/0",
                |           "locations": [],
                |           "path": [
                |             "issuesByKeys",
                |             0,
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
                |     },
                |     {
                |       "path": [
                |         "issuesByKeys",
                |         1
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Frank"
                |         }
                |       }
                |     },
                |     {
                |       "path": [
                |         "issuesByKeys",
                |         2
                |       ],
                |       "errors": [
                |         {
                |           "message": "No user: ari:cloud:identity::user/10",
                |           "locations": [],
                |           "path": [
                |             "issuesByKeys",
                |             2,
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
