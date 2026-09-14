// @formatter:off
package graphql.nadel.tests.next.fixtures.introspection

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<GoodFaithIntrospectionDisabledTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class GoodFaithIntrospectionDisabledTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * {
     *   first: __type(name: "Echo") {
     *     name
     *   }
     *   second: __type(name: "Echo") {
     *     name
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
            )

    /**
     * ```json
     * {
     *   "data": {
     *     "first": {
     *       "name": "Echo"
     *     },
     *     "second": {
     *       "name": "Echo"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "first": {
            |       "name": "Echo"
            |     },
            |     "second": {
            |       "name": "Echo"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
