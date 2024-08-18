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
                |     ... @defer {
                |       rename__slowRenamedString__slowString: slowString
                |       __typename__rename__slowRenamedString: __typename
                |     }
                |     ... @defer {
                |       rename__anotherSlowRenamedString__anotherSlowString: anotherSlowString
                |       __typename__rename__anotherSlowRenamedString: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "defer": {
                |       "rename__fastRenamedString__fastString": "this is the fast string (not deferred)",
                |       "__typename__rename__fastRenamedString": "DeferApi"
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
                    |         "anotherSlowRenamedString": "this is the other slow string (deferred)"
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
                    |         "defer"
                    |       ],
                    |       "data": {
                    |         "slowRenamedString": "this is the slow string (deferred)"
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
            |       "fastRenamedString": "this is the fast string (not deferred)"
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
                |         "anotherSlowRenamedString": "this is the other slow string (deferred)"
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
                |         "defer"
                |       ],
                |       "data": {
                |         "slowRenamedString": "this is the slow string (deferred)"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
