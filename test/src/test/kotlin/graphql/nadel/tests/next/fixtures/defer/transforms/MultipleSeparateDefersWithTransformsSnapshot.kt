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
    graphql.nadel.tests.next.update<MultipleSeparateDefersWithTransforms>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class MultipleSeparateDefersWithTransformsSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   defer {
                |     rename__fastRenamedString__fastString: fastString
                |     __typename__rename__fastRenamedString: __typename
                |     rename__slowRenamedString__slowString: slowString
                |     __typename__rename__slowRenamedString: __typename
                |     rename__anotherSlowRenamedString__anotherSlowString: anotherSlowString
                |     __typename__rename__anotherSlowRenamedString: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "defer": {
                |       "rename__fastRenamedString__fastString": "this is the fast string (not deferred)",
                |       "__typename__rename__fastRenamedString": "DeferApi",
                |       "rename__slowRenamedString__slowString": "this is the slow string (deferred)",
                |       "__typename__rename__slowRenamedString": "DeferApi",
                |       "rename__anotherSlowRenamedString__anotherSlowString": "this is the other slow string (deferred)",
                |       "__typename__rename__anotherSlowRenamedString": "DeferApi"
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
     *     "defer": {
     *       "fastRenamedString": "this is the fast string (not deferred)",
     *       "slowRenamedString": "this is the slow string (deferred)",
     *       "anotherSlowRenamedString": "this is the other slow string (deferred)"
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
            |       "fastRenamedString": "this is the fast string (not deferred)",
            |       "slowRenamedString": "this is the slow string (deferred)",
            |       "anotherSlowRenamedString": "this is the other slow string (deferred)"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
