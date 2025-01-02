// @formatter:off
package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`typename is correctly passed on and artificial typename is removed`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `typename is correctly passed on and artificial typename is removed snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   issues {
                |     __typename
                |     id
                |     ... on Issue {
                |       authorIds
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "__typename": "Issue",
                |         "id": "ISSUE-1",
                |         "authorIds": [
                |           "USER-1",
                |           "USER-2"
                |         ]
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "ISSUE-2",
                |         "authorIds": [
                |           "USER-3"
                |         ]
                |       }
                |     ]
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
     *     "issues": [
     *       {
     *         "__typename": "Issue",
     *         "id": "ISSUE-1",
     *         "authorIds": [
     *           "USER-1",
     *           "USER-2"
     *         ]
     *       },
     *       {
     *         "__typename": "Issue",
     *         "id": "ISSUE-2",
     *         "authorIds": [
     *           "USER-3"
     *         ]
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issues": [
            |       {
            |         "__typename": "Issue",
            |         "id": "ISSUE-1",
            |         "authorIds": [
            |           "USER-1",
            |           "USER-2"
            |         ]
            |       },
            |       {
            |         "__typename": "Issue",
            |         "id": "ISSUE-2",
            |         "authorIds": [
            |           "USER-3"
            |         ]
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
