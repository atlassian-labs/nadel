// @formatter:off
package graphql.nadel.tests.next.fixtures.rename

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<RenamedSharedInputTypeTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class RenamedSharedInputTypeTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "confluence_something",
                query = """
                | query (${'$'}v0: PathType!) {
                |   something {
                |     users {
                |       profilePicture {
                |         path(type: ${'$'}v0)
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ABSOLUTE"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "something": {
                |       "users": [
                |         {
                |           "profilePicture": {
                |             "path": "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70"
                |           }
                |         }
                |       ]
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
     *     "something": {
     *       "users": [
     *         {
     *           "profilePicture": {
     *             "path": "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70"
     *           }
     *         }
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
            |     "something": {
            |       "users": [
            |         {
            |           "profilePicture": {
            |             "path": "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70"
            |           }
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
