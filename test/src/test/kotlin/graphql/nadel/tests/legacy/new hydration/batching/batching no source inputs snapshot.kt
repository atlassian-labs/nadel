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
    graphql.nadel.tests.next.update<`batching no source inputs`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class `batching no source inputs snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "activity",
                query = """
                | {
                |   activity {
                |     __typename__batch_hydration__content: __typename
                |     batch_hydration__content__contentIds: contentIds
                |     batch_hydration__content__contentIds: contentIds
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "activity": [
                |       {
                |         "batch_hydration__content__contentIds": [],
                |         "__typename__batch_hydration__content": "Activity"
                |       },
                |       {
                |         "batch_hydration__content__contentIds": [],
                |         "__typename__batch_hydration__content": "Activity"
                |       },
                |       {
                |         "batch_hydration__content__contentIds": [
                |           "issue/7496",
                |           "comment/9001"
                |         ],
                |         "__typename__batch_hydration__content": "Activity"
                |       },
                |       {
                |         "batch_hydration__content__contentIds": [
                |           "issue/1234",
                |           "comment/1234"
                |         ],
                |         "__typename__batch_hydration__content": "Activity"
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
                |   commentsByIds(ids: ["comment/9001", "comment/1234"]) {
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
                |         "content": "It's over 9000",
                |         "batch_hydration__content__id": "comment/9001"
                |       },
                |       {
                |         "__typename": "Comment",
                |         "id": "comment/1234",
                |         "content": "One Two Three Four",
                |         "batch_hydration__content__id": "comment/1234"
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
                |   issuesByIds(ids: ["issue/7496", "issue/1234"]) {
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
                |         "id": "issue/7496",
                |         "title": "Seven Four Nine Six",
                |         "batch_hydration__content__id": "issue/7496"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "issue/1234",
                |         "title": "One Two Three Four",
                |         "batch_hydration__content__id": "issue/1234"
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
     *         "content": []
     *       },
     *       {
     *         "content": []
     *       },
     *       {
     *         "content": [
     *           {
     *             "__typename": "Issue",
     *             "id": "issue/7496",
     *             "title": "Seven Four Nine Six"
     *           },
     *           {
     *             "__typename": "Comment",
     *             "id": "comment/9001",
     *             "content": "It's over 9000"
     *           }
     *         ]
     *       },
     *       {
     *         "content": [
     *           {
     *             "__typename": "Issue",
     *             "id": "issue/1234",
     *             "title": "One Two Three Four"
     *           },
     *           {
     *             "__typename": "Comment",
     *             "id": "comment/1234",
     *             "content": "One Two Three Four"
     *           }
     *         ]
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
            |         "content": []
            |       },
            |       {
            |         "content": []
            |       },
            |       {
            |         "content": [
            |           {
            |             "__typename": "Issue",
            |             "id": "issue/7496",
            |             "title": "Seven Four Nine Six"
            |           },
            |           {
            |             "__typename": "Comment",
            |             "id": "comment/9001",
            |             "content": "It's over 9000"
            |           }
            |         ]
            |       },
            |       {
            |         "content": [
            |           {
            |             "__typename": "Issue",
            |             "id": "issue/1234",
            |             "title": "One Two Three Four"
            |           },
            |           {
            |             "__typename": "Comment",
            |             "id": "comment/1234",
            |             "content": "One Two Three Four"
            |           }
            |         ]
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
