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
    graphql.nadel.tests.next.update<`synthetic hydration call with two argument values from original field arguments`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `synthetic hydration call with two argument values from original field arguments snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   issues {
                |     __typename__batch_hydration__author: __typename
                |     batch_hydration__author__authorId: authorId
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "id": "ISSUE-1",
                |         "batch_hydration__author__authorId": "USER-1",
                |         "__typename__batch_hydration__author": "Issue"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "UserService",
                query = """
                | query (${'$'}v0: String, ${'$'}v1: Int, ${'$'}v2: [ID]) {
                |   usersQuery {
                |     usersByIds(extraArg1: ${'$'}v0, extraArg2: ${'$'}v1, id: ${'$'}v2) {
                |       batch_hydration__author__id: id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "extraArg1",
                |   "v1": 10,
                |   "v2": [
                |     "USER-1"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "usersQuery": {
                |       "usersByIds": [
                |         {
                |           "name": "User 1",
                |           "batch_hydration__author__id": "USER-1"
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
     *     "issues": [
     *       {
     *         "id": "ISSUE-1",
     *         "author": {
     *           "name": "User 1"
     *         }
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
            |         "id": "ISSUE-1",
            |         "author": {
            |           "name": "User 1"
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
