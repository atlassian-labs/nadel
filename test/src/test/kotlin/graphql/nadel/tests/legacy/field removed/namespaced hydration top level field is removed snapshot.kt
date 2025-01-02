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
    graphql.nadel.tests.next.update<`namespaced hydration top level field is removed`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `namespaced hydration top level field is removed snapshot` : TestSnapshot() {
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
                |       "__typename__hydration__comment": "Issue",
                |       "hydration__comment__commentId": "C1",
                |       "id": "C1"
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
     *     "issueById": {
     *       "id": "C1",
     *       "comment": null
     *     }
     *   },
     *   "errors": [
     *     {
     *       "message": "field `CommentApi.commentById` has been removed by
     * RemoveFieldTestTransform",
     *       "locations": [],
     *       "extensions": {
     *         "classification": "DataFetchingException"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issueById": {
            |       "id": "C1",
            |       "comment": null
            |     }
            |   },
            |   "errors": [
            |     {
            |       "message": "field `CommentApi.commentById` has been removed by RemoveFieldTestTransform",
            |       "locations": [],
            |       "extensions": {
            |         "classification": "DataFetchingException"
            |       }
            |     }
            |   ]
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
