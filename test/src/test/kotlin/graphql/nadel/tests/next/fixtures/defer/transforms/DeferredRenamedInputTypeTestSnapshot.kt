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
    graphql.nadel.tests.next.update<DeferredRenamedInputTypeTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class DeferredRenamedInputTypeTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "confluence_legacy",
                query = """
                | query (${'$'}v0: PathType!) {
                |   me {
                |     profilePicture {
                |       path(type: ${'$'}v0)
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
                |     "me": {
                |       "profilePicture": {
                |         "path": "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70"
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
     *     "me": {
     *       "profilePicture": {
     *         "path": "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70"
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
            |     "me": {
            |       "profilePicture": {
            |         "path": "https://atlassian.net/wiki/aa-avatar/5ee0a4ef55749e0ab6e0fb70"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
