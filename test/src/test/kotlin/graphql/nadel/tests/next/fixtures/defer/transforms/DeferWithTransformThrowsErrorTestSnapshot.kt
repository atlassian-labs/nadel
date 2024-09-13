package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.fixtures.defer.DeferThrowsErrorTest
import graphql.nadel.tests.next.fixtures.defer.DeferWithTransformThrowsErrorTest
import graphql.nadel.tests.next.listOfJsonStrings

private suspend fun main() {
    graphql.nadel.tests.next.update<DeferWithTransformThrowsErrorTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class DeferWithTransformThrowsErrorTestSnapshot : TestSnapshot() {
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
            variables = " {}",
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