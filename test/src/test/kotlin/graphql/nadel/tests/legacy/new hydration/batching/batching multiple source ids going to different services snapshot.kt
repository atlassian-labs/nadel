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
    graphql.nadel.tests.next.update<`batching multiple source ids going to different services`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batching multiple source ids going to different services snapshot` : TestSnapshot() {
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
                |         "__typename__batch_hydration__content": "Activity",
                |         "batch_hydration__content__contentIds": [
                |           "issue/4000",
                |           "comment/5000",
                |           "comment/6000"
                |         ]
                |       },
                |       {
                |         "__typename__batch_hydration__content": "Activity",
                |         "batch_hydration__content__contentIds": [
                |           "issue/8080"
                |         ]
                |       },
                |       {
                |         "__typename__batch_hydration__content": "Activity",
                |         "batch_hydration__content__contentIds": [
                |           "issue/7496",
                |           "comment/9001"
                |         ]
                |       },
                |       {
                |         "__typename__batch_hydration__content": "Activity",
                |         "batch_hydration__content__contentIds": [
                |           "issue/1234",
                |           "comment/1234"
                |         ]
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
                |   commentsByIds(ids: ["comment/5000", "comment/6000", "comment/9001", "comment/1234"]) {
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
                |         "id": "comment/5000",
                |         "batch_hydration__content__id": "comment/5000",
                |         "content": "Five Thousand"
                |       },
                |       {
                |         "__typename": "Comment",
                |         "id": "comment/6000",
                |         "batch_hydration__content__id": "comment/6000",
                |         "content": "Six Thousand"
                |       },
                |       {
                |         "__typename": "Comment",
                |         "id": "comment/9001",
                |         "batch_hydration__content__id": "comment/9001",
                |         "content": "It's over 9000"
                |       },
                |       {
                |         "__typename": "Comment",
                |         "id": "comment/1234",
                |         "batch_hydration__content__id": "comment/1234",
                |         "content": "One Two Three Four"
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
                |   issuesByIds(ids: ["issue/4000", "issue/8080", "issue/7496", "issue/1234"]) {
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
                |         "id": "issue/4000",
                |         "batch_hydration__content__id": "issue/4000",
                |         "title": "Four Thousand"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "issue/8080",
                |         "batch_hydration__content__id": "issue/8080",
                |         "title": "Eighty Eighty"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "issue/7496",
                |         "batch_hydration__content__id": "issue/7496",
                |         "title": "Seven Four Nine Six"
                |       },
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
     *         "content": [
     *           {
     *             "__typename": "Issue",
     *             "id": "issue/4000",
     *             "title": "Four Thousand"
     *           },
     *           {
     *             "__typename": "Comment",
     *             "id": "comment/5000",
     *             "content": "Five Thousand"
     *           },
     *           {
     *             "__typename": "Comment",
     *             "id": "comment/6000",
     *             "content": "Six Thousand"
     *           }
     *         ]
     *       },
     *       {
     *         "content": [
     *           {
     *             "__typename": "Issue",
     *             "id": "issue/8080",
     *             "title": "Eighty Eighty"
     *           }
     *         ]
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
            |         "content": [
            |           {
            |             "__typename": "Issue",
            |             "id": "issue/4000",
            |             "title": "Four Thousand"
            |           },
            |           {
            |             "__typename": "Comment",
            |             "id": "comment/5000",
            |             "content": "Five Thousand"
            |           },
            |           {
            |             "__typename": "Comment",
            |             "id": "comment/6000",
            |             "content": "Six Thousand"
            |           }
            |         ]
            |       },
            |       {
            |         "content": [
            |           {
            |             "__typename": "Issue",
            |             "id": "issue/8080",
            |             "title": "Eighty Eighty"
            |           }
            |         ]
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
