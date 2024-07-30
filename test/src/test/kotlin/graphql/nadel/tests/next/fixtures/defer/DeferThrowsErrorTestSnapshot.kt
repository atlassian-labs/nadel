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
    graphql.nadel.tests.next.update<DeferThrowsErrorTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class DeferThrowsErrorTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   defer {
     *     hello
     *     ... @defer(label: "slow-defer") {
     *       slow
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
                service = "defer",
                query = """
                | {
                |   defer {
                |     hello
                |     ... @defer(label: "slow-defer") {
                |       slow
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "defer": {
                |       "hello": "helloString"
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
                    |         "defer"
                    |       ],
                    |       "label": "slow-defer",
                    |       "errors": [
                    |         {
                    |           "message": "Exception while fetching data (/defer/slow) : An error occurred while fetching 'slow'",
                    |           "locations": [
                    |             {
                    |               "line": 5,
                    |               "column": 7
                    |             }
                    |           ],
                    |           "path": [
                    |             "defer",
                    |             "slow"
                    |           ],
                    |           "extensions": {
                    |             "classification": "DataFetchingException"
                    |           }
                    |         }
                    |       ],
                    |       "data": {
                    |         "slow": null
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
        )

    /**
     * Combined Result
     *
     * ```json
     * {
     *   "data": {
     *     "defer": {
     *       "hello": "helloString",
     *       "slow": null
     *     }
     *   },
     *   "errors": [
     *     {
     *       "message": "Exception while fetching data (/defer/slow) : An error occurred while
     * fetching 'slow'",
     *       "locations": [
     *         {
     *           "line": 5,
     *           "column": 7
     *         }
     *       ],
     *       "path": [
     *         "defer",
     *         "slow"
     *       ],
     *       "extensions": {
     *         "classification": "DataFetchingException"
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
            |     "defer": {
            |       "hello": "helloString"
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
                |         "defer"
                |       ],
                |       "label": "slow-defer",
                |       "errors": [
                |         {
                |           "message": "Exception while fetching data (/defer/slow) : An error occurred while fetching 'slow'",
                |           "locations": [
                |             {
                |               "line": 5,
                |               "column": 7
                |             }
                |           ],
                |           "path": [
                |             "defer",
                |             "slow"
                |           ],
                |           "extensions": {
                |             "classification": "DataFetchingException"
                |           }
                |         }
                |       ],
                |       "data": {
                |         "slow": null
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
