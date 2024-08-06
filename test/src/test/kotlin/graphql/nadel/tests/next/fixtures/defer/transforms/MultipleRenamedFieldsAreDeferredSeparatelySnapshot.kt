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
    graphql.nadel.tests.next.update<MultipleRenamedFieldsAreDeferredSeparately>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class MultipleRenamedFieldsAreDeferredSeparatelySnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   defer {
                |     hello
                |     ... @defer(label: "defer1") {
                |       rename__overallString__underlyingString: underlyingString
                |       __typename__rename__overallString: __typename
                |     }
                |     ... @defer(label: "defer2") {
                |       rename__overallString__underlyingString: underlyingString
                |       __typename__rename__overallString: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "defer": {
                |       "hello": "hello there"
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
                    |       "label": "defer2",
                    |       "data": {
                    |         "rename__overallString__underlyingString": "deferred string 1",
                    |         "__typename__rename__overallString": "DeferApi"
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
                    |       "label": "defer1",
                    |       "data": {
                    |         "rename__overallString__underlyingString": "deferred string 1",
                    |         "__typename__rename__overallString": "DeferApi"
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
     *       "hello": "hello there",
     *       "rename__overallString__underlyingString": "deferred string 1",
     *       "__typename__rename__overallString": "DeferApi"
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
            |       "hello": "hello there"
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
                |       "label": "defer2",
                |       "data": {
                |         "rename__overallString__underlyingString": "deferred string 1",
                |         "__typename__rename__overallString": "DeferApi"
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
                |       "label": "defer1",
                |       "data": {
                |         "rename__overallString__underlyingString": "deferred string 1",
                |         "__typename__rename__overallString": "DeferApi"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
