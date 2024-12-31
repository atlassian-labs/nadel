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
    graphql.nadel.tests.next.update<`namespaced result map has correct entries on partial failure`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `namespaced result map has correct entries on partial failure snapshot` :
        TestSnapshot() {
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
                |       "message": "Error",
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
                | query {
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
     *   "data": {
     *     "issue": {
     *       "getIssue": {
     *         "text": "Foo"
     *       },
     *       "search": null
     *     }
     *   },
     *   "errors": [
     *     {
     *       "message": "Error",
     *       "locations": [],
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
            |     "issue": {
            |       "getIssue": {
            |         "text": "Foo"
            |       },
            |       "search": null
            |     }
            |   },
            |   "errors": [
            |     {
            |       "message": "Error",
            |       "locations": [],
            |       "extensions": {
            |         "classification": "DataFetchingException"
            |       }
            |     }
            |   ]
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
