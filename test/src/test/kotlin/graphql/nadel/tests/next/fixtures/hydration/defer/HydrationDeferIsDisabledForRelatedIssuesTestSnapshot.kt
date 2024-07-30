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
    graphql.nadel.tests.next.update<HydrationDeferIsDisabledForRelatedIssuesTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferIsDisabledForRelatedIssuesTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issueByKey(key: "GQLGW-2") { # Not a list
     *     key
     *     ... @defer {
     *       assignee { # Should defer
     *         name
     *       }
     *     }
     *     related { # Is a list
     *       ... @defer {
     *         assignee { # Should NOT defer
     *           name
     *         }
     *       }
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
                service = "issues",
                query = """
                | {
                |   issueByKey(key: "GQLGW-2") {
                |     key
                |     hydration__assignee__assigneeId: assigneeId
                |     __typename__hydration__assignee: __typename
                |     related {
                |       hydration__assignee__assigneeId: assigneeId
                |       __typename__hydration__assignee: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueByKey": {
                |       "key": "GQLGW-2",
                |       "hydration__assignee__assigneeId": "ari:cloud:identity::user/2",
                |       "__typename__hydration__assignee": "Issue",
                |       "related": [
                |         {
                |           "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |           "__typename__hydration__assignee": "Issue"
                |         }
                |       ]
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
                |       "name": "Franklin"
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
        )

    /**
     * Combined Result
     *
     * ```json
     * {
     *   "data": {
     *     "issueByKey": {
     *       "key": "GQLGW-2",
     *       "related": [
     *         {
     *           "assignee": {
     *             "name": "Franklin"
     *           }
     *         }
     *       ],
     *       "assignee": {
     *         "name": "Tom"
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
            |     "issueByKey": {
            |       "key": "GQLGW-2",
            |       "related": [
            |         {
            |           "assignee": {
            |             "name": "Franklin"
            |           }
            |         }
            |       ]
            |     }
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
                |       "data": {
                |         "assignee": {
                |           "name": "Tom"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
