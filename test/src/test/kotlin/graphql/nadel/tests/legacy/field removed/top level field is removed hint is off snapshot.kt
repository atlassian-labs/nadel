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
    graphql.nadel.tests.next.update<`top level field is removed hint is off`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `top level field is removed hint is off snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "CommentService",
                query = """
                | {
                |   uuid_typename: __typename
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "uuid_typename": "Query"
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
     *     "commentById": null
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
            |     "commentById": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
