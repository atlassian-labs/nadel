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
    graphql.nadel.tests.next.update<HydrationDeferInListTopLevelTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferInListTopLevelTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issues {
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
                |     "issues": [
                |       {
                |         "key": "GQLGW-1",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__hydration__assignee": "Issue"
                |       },
                |       {
                |         "key": "GQLGW-2",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/2",
                |         "__typename__hydration__assignee": "Issue"
                |       },
                |       {
                |         "key": "GQLGW-3",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__hydration__assignee": "Issue"
                |       },
                |       {
                |         "key": "GQLGW-4",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/3",
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
                |   userById(id: "ari:cloud:identity::user/2") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Tom"
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
                |   userById(id: "ari:cloud:identity::user/3") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Lin"
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
     *     "issues": [
     *       {
     *         "key": "GQLGW-1",
     *         "assignee": {
     *           "name": "Frank"
     *         }
     *       },
     *       {
     *         "key": "GQLGW-2",
     *         "assignee": {
     *           "name": "Tom"
     *         }
     *       },
     *       {
     *         "key": "GQLGW-3",
     *         "assignee": {
     *           "name": "Frank"
     *         }
     *       },
     *       {
     *         "key": "GQLGW-4",
     *         "assignee": {
     *           "name": "Lin"
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
            |         "key": "GQLGW-1"
            |       },
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
                |         "issues",
                |         0
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Frank"
                |         }
                |       }
                |     },
                |     {
                |       "path": [
                |         "issues",
                |         1
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Tom"
                |         }
                |       }
                |     },
                |     {
                |       "path": [
                |         "issues",
                |         2
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Frank"
                |         }
                |       }
                |     },
                |     {
                |       "path": [
                |         "issues",
                |         3
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Lin"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
