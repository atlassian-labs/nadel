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
public class QueryContainingDeferButNonDeferredFieldIsRenamedTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   defer {
                |     rename__overallString__underlyingString: underlyingString
                |     __typename__rename__overallString: __typename
                |     ... @defer {
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
                |       "rename__overallString__underlyingString": "this is the (non-deferred) renamed string",
                |       "__typename__rename__overallString": "DeferApi"
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
                    |       "data": {
                    |         "slow": "this is the deferred string"
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
     *       "overallString": "this is the (non-deferred) renamed string",
     *       "slow": "this is the deferred string"
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
            |       "overallString": "this is the (non-deferred) renamed string"
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
                |       "data": {
                |         "slow": "this is the deferred string"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
