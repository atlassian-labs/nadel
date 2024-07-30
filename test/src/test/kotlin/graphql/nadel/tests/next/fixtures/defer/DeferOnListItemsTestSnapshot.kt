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
    graphql.nadel.tests.next.update<DeferOnListItemsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class DeferOnListItemsTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   defer {
     *       list {
     *         fast
     *         ... @defer {
     *           slow
     *         }
     *       }
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
                |     list {
                |       fast
                |       ... @defer {
                |         slow
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "defer": {
                |       "list": [
                |         {
                |           "fast": "fastString"
                |         },
                |         {
                |           "fast": "fastString"
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
                    |         "defer",
                    |         "list",
                    |         1
                    |       ],
                    |       "data": {
                    |         "slow": "slowString"
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                    """
                    | {
                    |   "hasNext": true,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "defer",
                    |         "list",
                    |         0
                    |       ],
                    |       "data": {
                    |         "slow": "slowString"
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
     *       "list": [
     *         {
     *           "fast": "fastString",
     *           "slow": "slowString"
     *         },
     *         {
     *           "fast": "fastString",
     *           "slow": "slowString"
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "defer": {
            |       "list": [
            |         {
            |           "fast": "fastString"
            |         },
            |         {
            |           "fast": "fastString"
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
                |         "defer",
                |         "list",
                |         1
                |       ],
                |       "data": {
                |         "slow": "slowString"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                """
                | {
                |   "hasNext": true,
                |   "incremental": [
                |     {
                |       "path": [
                |         "defer",
                |         "list",
                |         0
                |       ],
                |       "data": {
                |         "slow": "slowString"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
