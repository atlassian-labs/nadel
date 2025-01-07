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
    graphql.nadel.tests.next.update<`dynamic service resolution handles complex fragments`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `dynamic service resolution handles complex fragments snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "RepoService",
                query = """
                | {
                |   node(id: "pull-request:id-123") {
                |     id
                |     ... on PullRequest {
                |       author {
                |         avatarUrl
                |         name
                |       }
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
                |       "description": "this is a pull request",
                |       "author": {
                |         "name": "I'm an User",
                |         "avatarUrl": "https://avatar.acme.com/user-123"
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
     *     "node": {
     *       "id": "pull-request:id-123",
     *       "description": "this is a pull request",
     *       "author": {
     *         "name": "I'm an User",
     *         "avatarUrl": "https://avatar.acme.com/user-123"
     *       }
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
            |       "description": "this is a pull request",
            |       "author": {
            |         "name": "I'm an User",
            |         "avatarUrl": "https://avatar.acme.com/user-123"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
