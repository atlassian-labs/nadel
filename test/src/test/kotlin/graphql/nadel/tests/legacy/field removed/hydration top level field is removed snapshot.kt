// @formatter:off
package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration top level field is removed`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration top level field is removed snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | {
                |   issueById(id: "C1") {
                |     __typename__hydration__comment: __typename
                |     hydration__comment__commentId: commentId
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "id": "C1",
                |       "hydration__comment__commentId": "C1",
                |       "__typename__hydration__comment": "Issue"
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
     *   "errors": [
     *     {
     *       "message": "field `Query.commentById` has been removed by RemoveFieldTestTransform",
     *       "locations": [],
     *       "extensions": {
     *         "classification": "DataFetchingException"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "issueById": {
     *       "id": "C1",
     *       "comment": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "field `Query.commentById` has been removed by RemoveFieldTestTransform",
            |       "locations": [],
            |       "extensions": {
            |         "classification": "DataFetchingException"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "issueById": {
            |       "id": "C1",
            |       "comment": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
