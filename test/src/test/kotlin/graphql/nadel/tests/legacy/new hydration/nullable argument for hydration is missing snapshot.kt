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
    graphql.nadel.tests.next.update<`nullable argument for hydration is missing`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `nullable argument for hydration is missing snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "boards",
                query = """
                | {
                |   board {
                |     __typename__hydration__issue: __typename
                |     hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "board": {
                |       "hydration__issue__issueId": "ISSUE-1",
                |       "__typename__hydration__issue": "Board"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "comments",
                query = """
                | query (${'$'}v0: ID!) {
                |   comments(cloudId: ${'$'}v0) {
                |     totalCount
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "CLOUD_ID-1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "comments": {
                |       "totalCount": 10
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | query (${'$'}v0: ID) {
                |   issue(id: ${'$'}v0) {
                |     __typename__hydration__comments: __typename
                |     hydration__comments__cloudId: cloudId
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ISSUE-1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "hydration__comments__cloudId": "CLOUD_ID-1",
                |       "__typename__hydration__comments": "Issue"
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
     *     "board": {
     *       "issue": {
     *         "comments": {
     *           "totalCount": 10
     *         }
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
            |     "board": {
            |       "issue": {
            |         "comments": {
            |           "totalCount": 10
            |         }
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
