// @formatter:off
package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.tests.next.ExpectedNadelResult
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
public class MultipleRenamedFieldsAreDeferredTogetherSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   defer {
                |     hello
                |     rename__overallString__underlyingString: underlyingString
                |     __typename__rename__overallString: __typename
                |     rename__overallString2__underlyingString2: underlyingString2
                |     __typename__rename__overallString2: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "defer": {
                |       "hello": "hello there",
                |       "rename__overallString__underlyingString": "deferred string 1",
                |       "__typename__rename__overallString": "DeferApi",
                |       "rename__overallString2__underlyingString2": "deferred string 2",
                |       "__typename__rename__overallString2": "DeferApi"
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
     *       "hello": "hello there",
     *       "overallString": "deferred string 1",
     *       "overallString2": "deferred string 2"
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
            |       "hello": "hello there",
            |       "overallString": "deferred string 1",
            |       "overallString2": "deferred string 2"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
