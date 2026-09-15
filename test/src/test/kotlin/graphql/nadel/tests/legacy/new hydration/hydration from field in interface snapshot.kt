// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration from field in interface`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration from field in interface snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | query (${'$'}v0: ID) {
                |   issue(id: ${'$'}v0) {
                |     __typename__hydration__issueAuthor: __typename
                |     hydration__issueAuthor__author: author {
                |       userId
                |     }
                |     title
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "title": "Issue 1",
                |       "hydration__issueAuthor__author": {
                |         "userId": "1001"
                |       },
                |       "__typename__hydration__issueAuthor": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | query (${'$'}v0: ID!) {
                |   user(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1001"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "user": {
                |       "name": "McUser Face"
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
     *       "title": "Issue 1",
     *       "issueAuthor": {
     *         "name": "McUser Face"
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
            |     "issue": {
            |       "title": "Issue 1",
            |       "issueAuthor": {
            |         "name": "McUser Face"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
