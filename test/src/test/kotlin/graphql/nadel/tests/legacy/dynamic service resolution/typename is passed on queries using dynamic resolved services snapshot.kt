// @formatter:off
package graphql.nadel.tests.legacy.`dynamic service resolution`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`typename is passed on queries using dynamic resolved services`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `typename is passed on queries using dynamic resolved services snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "RepoService",
                query = """
                | {
                |   node(id: "pull-request:id-123") {
                |     __typename
                |     id
                |     ... on PullRequest {
                |       description
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "node": {
                |       "__typename": "PullRequest",
                |       "id": "pull-request:id-123",
                |       "description": "this is a pull request"
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
     *     "node": {
     *       "__typename": "PullRequest",
     *       "id": "pull-request:id-123",
     *       "description": "this is a pull request"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "node": {
            |       "__typename": "PullRequest",
            |       "id": "pull-request:id-123",
            |       "description": "this is a pull request"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
