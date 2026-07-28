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
    graphql.nadel.tests.next.update<NamedFragmentDeferAtRootTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class NamedFragmentDeferAtRootTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query RootLevelFragmentDeferQuery {
     *   greeting
     *   ...RecommendationsDeferred @defer(label: "recommendations")
     * }
     *
     * fragment RecommendationsDeferred on Query {
     *   recommendations {
     *     items
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
                | query RootLevelFragmentDeferQuery {
                |   ... @defer(label: "recommendations") {
                |     recommendations {
                |       items
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
                    |       "label": "recommendations",
                    |       "data": {
                    |         "recommendations": {
                    |           "items": [
                    |             "first",
                    |             "second"
                    |           ]
                    |         }
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
            ExpectedServiceCall(
                service = "defer",
                query = """
                | query RootLevelFragmentDeferQuery {
                |   greeting
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "greeting": "helloString"
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
     *     "greeting": "helloString",
     *     "recommendations": {
     *       "items": [
     *         "first",
     *         "second"
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
            |     "greeting": "helloString",
            |     "recommendations": null
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
                |       "path": [],
                |       "label": "recommendations",
                |       "data": {
                |         "recommendations": {
                |           "items": [
                |             "first",
                |             "second"
                |           ]
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
