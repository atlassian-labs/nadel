// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResponse
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
public class HydrationDeferIsDisabledTestSnapshot : TestSnapshot() {
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
                response = """
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
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
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
                response = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Franklin"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
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
                response = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Franklin"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
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
                response = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Tom"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
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
     *           "name": "Franklin"
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
     *           "name": "Franklin"
     *         }
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val response: ExpectedNadelResponse = ExpectedNadelResponse(
            response = """
            | {
            |   "data": {
            |     "issues": [
            |       {
            |         "key": "GQLGW-1",
            |         "assignee": {
            |           "name": "Franklin"
            |         }
            |       },
            |       {
            |         "key": "GQLGW-2",
            |         "assignee": {
            |           "name": "Tom"
            |         }
            |       },
            |       {
            |         "key": "GQLGW-3",
            |         "assignee": {
            |           "name": "Franklin"
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResponses = listOfJsonStrings(
            ),
        )
}
