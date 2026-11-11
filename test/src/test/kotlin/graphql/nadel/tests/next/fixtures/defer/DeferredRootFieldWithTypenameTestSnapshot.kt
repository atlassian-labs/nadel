// @formatter:off
package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<DeferredRootFieldWithTypenameTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class DeferredRootFieldWithTypenameTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query Repro {
     *   ... @defer(label: "Body") {
     *     me {
     *       user { id }
     *     }
     *     __typename
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
                service = "__introspection",
                query = """
                | query Repro {
                |   ... @defer(label: "Body") {
                |     __typename
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
                    |       "label": "Body",
                    |       "data": {
                    |         "__typename": "Query"
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | query Repro {
                |   ... @defer(label: "Body") {
                |     me {
                |       user {
                |         id
                |       }
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
                    |       "label": "Body",
                    |       "data": {
                    |         "me": {
                    |           "user": {
                    |             "id": "user-1"
                    |           }
                    |         }
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
     *     "__typename": "Query",
     *     "me": {
     *       "user": {
     *         "id": "user-1"
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
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
                |       "label": "Body",
                |       "data": {
                |         "__typename": "Query",
                |         "me": {
                |           "user": {
                |             "id": "user-1"
                |           }
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
