// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`batching absent source input`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batching absent source input snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "activity",
                query = """
                | {
                |   activity {
                |     __typename__batch_hydration__content: __typename
                |     batch_hydration__content__contentId: contentId
                |     batch_hydration__content__contentId: contentId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "activity": [
                |       {
                |         "__typename__batch_hydration__content": "Activity"
                |       },
                |       {
                |         "__typename__batch_hydration__content": "Activity",
                |         "batch_hydration__content__contentId": ""
                |       },
                |       {
                |         "__typename__batch_hydration__content": "Activity",
                |         "batch_hydration__content__contentId": "comment/9001"
                |       },
                |       {
                |         "__typename__batch_hydration__content": "Activity",
                |         "batch_hydration__content__contentId": "issue/1234"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "comments",
                query = """
                | {
                |   commentsByIds(ids: ["comment/9001"]) {
                |     __typename
                |     content
                |     id
                |     batch_hydration__content__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "commentsByIds": [
                |       {
                |         "__typename": "Comment",
                |         "id": "comment/9001",
                |         "batch_hydration__content__id": "comment/9001",
                |         "content": "It's over 9000"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issuesByIds(ids: ["issue/1234"]) {
                |     __typename
                |     id
                |     batch_hydration__content__id: id
                |     title
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issuesByIds": [
                |       {
                |         "__typename": "Issue",
                |         "id": "issue/1234",
                |         "batch_hydration__content__id": "issue/1234",
                |         "title": "One Two Three Four"
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
     *     "activity": [
     *       {
     *         "content": null
     *       },
     *       {
     *         "content": null
     *       },
     *       {
     *         "content": {
     *           "__typename": "Comment",
     *           "id": "comment/9001",
     *           "content": "It's over 9000"
     *         }
     *       },
     *       {
     *         "content": {
     *           "__typename": "Issue",
     *           "id": "issue/1234",
     *           "title": "One Two Three Four"
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
            |     "activity": [
            |       {
            |         "content": null
            |       },
            |       {
            |         "content": null
            |       },
            |       {
            |         "content": {
            |           "__typename": "Comment",
            |           "id": "comment/9001",
            |           "content": "It's over 9000"
            |         }
            |       },
            |       {
            |         "content": {
            |           "__typename": "Issue",
            |           "id": "issue/1234",
            |           "title": "One Two Three Four"
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
