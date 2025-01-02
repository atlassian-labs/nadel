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
    graphql.nadel.tests.next.update<`dynamic service resolution handles inline fragments from multiple services`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `dynamic service resolution handles inline fragments from multiple services snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "RepoService",
                query = """
                | {
                |   node(id: "pull-request:id-123") {
                |     __typename__type_filter__issueKey: __typename
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
                |       "id": "pull-request:id-123",
                |       "__typename__type_filter__issueKey": "PullRequest",
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
