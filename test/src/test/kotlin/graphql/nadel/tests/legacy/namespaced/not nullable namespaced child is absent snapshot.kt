// @formatter:off
package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`not nullable namespaced child is absent`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `not nullable namespaced child is absent snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueSearch",
                query = """
                | {
                |   issue {
                |     search {
                |       count
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Error on IssueSearch",
                |       "locations": [],
                |       "extensions": {
                |         "classification": "DataFetchingException"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "issue": null
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   issue {
                |     getIssue {
                |       text
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "getIssue": {
                |         "text": "Foo"
                |       }
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
     *   "errors": [
     *     {
     *       "message": "Error on IssueSearch",
     *       "locations": [],
     *       "extensions": {
     *         "classification": "DataFetchingException"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "issue": null
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Error on IssueSearch",
            |       "locations": [],
            |       "extensions": {
            |         "classification": "DataFetchingException"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "issue": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
