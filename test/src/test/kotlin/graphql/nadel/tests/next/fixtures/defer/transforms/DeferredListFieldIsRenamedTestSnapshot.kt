// @formatter:off
package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<DeferredListFieldIsRenamedTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class DeferredListFieldIsRenamedTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   ... @defer {
                |     issues {
                |       key
                |       assigneeId
                |       rename__awesomeIssueName__title: title
                |       __typename__rename__awesomeIssueName: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {},
                |   "hasNext": true
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                    """
                    | {
                    |   "hasNext": false,
                    |   "incremental": [
                    |     {
                    |       "path": [],
                    |       "data": {
                    |         "issues": [
                    |           {
                    |             "key": "GQLGW-1",
                    |             "assigneeId": "ari:cloud:identity::user/1",
                    |             "rename__awesomeIssueName__title": "Issue 1",
                    |             "__typename__rename__awesomeIssueName": "Issue"
                    |           },
                    |           {
                    |             "key": "GQLGW-2",
                    |             "assigneeId": "ari:cloud:identity::user/2",
                    |             "rename__awesomeIssueName__title": "Issue 2",
                    |             "__typename__rename__awesomeIssueName": "Issue"
                    |           },
                    |           {
                    |             "key": "GQLGW-3",
                    |             "assigneeId": "ari:cloud:identity::user/1",
                    |             "rename__awesomeIssueName__title": "Issue 3",
                    |             "__typename__rename__awesomeIssueName": "Issue"
                    |           },
                    |           {
                    |             "key": "GQLGW-4",
                    |             "assigneeId": "ari:cloud:identity::user/3",
                    |             "rename__awesomeIssueName__title": null,
                    |             "__typename__rename__awesomeIssueName": "Issue"
                    |           }
                    |         ]
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
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
     *         "assigneeId": "ari:cloud:identity::user/1",
     *         "awesomeIssueName": "Issue 1"
     *       },
     *       {
     *         "key": "GQLGW-2",
     *         "assigneeId": "ari:cloud:identity::user/2",
     *         "awesomeIssueName": "Issue 2"
     *       },
     *       {
     *         "key": "GQLGW-3",
     *         "assigneeId": "ari:cloud:identity::user/1",
     *         "awesomeIssueName": "Issue 3"
     *       },
     *       {
     *         "key": "GQLGW-4",
     *         "assigneeId": "ari:cloud:identity::user/3",
     *         "awesomeIssueName": null
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
            |     "issues": null
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
                |       "path": [],
                |       "data": {
                |         "issues": [
                |           {
                |             "key": "GQLGW-1",
                |             "assigneeId": "ari:cloud:identity::user/1",
                |             "awesomeIssueName": "Issue 1"
                |           },
                |           {
                |             "key": "GQLGW-2",
                |             "assigneeId": "ari:cloud:identity::user/2",
                |             "awesomeIssueName": "Issue 2"
                |           },
                |           {
                |             "key": "GQLGW-3",
                |             "assigneeId": "ari:cloud:identity::user/1",
                |             "awesomeIssueName": "Issue 3"
                |           },
                |           {
                |             "key": "GQLGW-4",
                |             "assigneeId": "ari:cloud:identity::user/3",
                |             "awesomeIssueName": null
                |           }
                |         ]
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
