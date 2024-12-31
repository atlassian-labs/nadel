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
    graphql.nadel.tests.next.update<`batch hydration instruction hook returns null`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batch hydration instruction hook returns null snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   issueById(id: "10000") {
                |     __typename__batch_hydration__collaborators: __typename
                |     batch_hydration__collaborators__collaboratorIds: collaboratorIds
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "key": "GQLGW-1000",
                |       "batch_hydration__collaborators__collaboratorIds": [
                |         "100",
                |         "NULL/1",
                |         "200"
                |       ],
                |       "__typename__batch_hydration__collaborators": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Users",
                query = """
                | {
                |   usersByIds(ids: ["100", "200"]) {
                |     __typename
                |     batch_hydration__collaborators__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "__typename": "User",
                |         "name": "John Doe",
                |         "batch_hydration__collaborators__id": "100"
                |       },
                |       {
                |         "__typename": "User",
                |         "name": "Joe",
                |         "batch_hydration__collaborators__id": "200"
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
     *     "issueById": {
     *       "key": "GQLGW-1000",
     *       "collaborators": [
     *         {
     *           "__typename": "User",
     *           "name": "John Doe"
     *         },
     *         null,
     *         {
     *           "__typename": "User",
     *           "name": "Joe"
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
            |     "issueById": {
            |       "key": "GQLGW-1000",
            |       "collaborators": [
            |         {
            |           "__typename": "User",
            |           "name": "John Doe"
            |         },
            |         null,
            |         {
            |           "__typename": "User",
            |           "name": "Joe"
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
