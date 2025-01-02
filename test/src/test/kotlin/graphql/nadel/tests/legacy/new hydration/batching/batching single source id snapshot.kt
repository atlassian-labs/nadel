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
    graphql.nadel.tests.next.update<`batching single source id`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batching single source id snapshot` : TestSnapshot() {
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
                |         "batch_hydration__content__contentId": "issue/4000",
                |         "__typename__batch_hydration__content": "Activity"
                |       },
                |       {
                |         "batch_hydration__content__contentId": "issue/8080",
                |         "__typename__batch_hydration__content": "Activity"
                |       },
                |       {
                |         "batch_hydration__content__contentId": "issue/7496",
                |         "__typename__batch_hydration__content": "Activity"
                |       },
                |       {
                |         "batch_hydration__content__contentId": "comment/1234",
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
                |   commentsByIds(ids: ["comment/1234"]) {
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
                |   issuesByIds(ids: ["issue/4000", "issue/8080", "issue/7496"]) {
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
                |         "title": "Four Thousand",
                |         "batch_hydration__content__id": "issue/4000"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "issue/8080",
                |         "title": "Eighty Eighty",
                |         "batch_hydration__content__id": "issue/8080"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "issue/7496",
                |         "title": "Seven Four Nine Six",
                |         "batch_hydration__content__id": "issue/7496"
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
     *         "content": {
     *           "__typename": "Issue",
     *           "id": "issue/4000",
     *           "title": "Four Thousand"
     *         }
     *       },
     *       {
     *         "content": {
     *           "__typename": "Issue",
     *           "id": "issue/8080",
     *           "title": "Eighty Eighty"
     *         }
     *       },
     *       {
     *         "content": {
     *           "__typename": "Issue",
     *           "id": "issue/7496",
     *           "title": "Seven Four Nine Six"
     *         }
     *       },
     *       {
     *         "content": {
     *           "__typename": "Comment",
     *           "id": "comment/1234",
     *           "content": "One Two Three Four"
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
            |         "content": {
            |           "__typename": "Issue",
            |           "id": "issue/4000",
            |           "title": "Four Thousand"
            |         }
            |       },
            |       {
            |         "content": {
            |           "__typename": "Issue",
            |           "id": "issue/8080",
            |           "title": "Eighty Eighty"
            |         }
            |       },
            |       {
            |         "content": {
            |           "__typename": "Issue",
            |           "id": "issue/7496",
            |           "title": "Seven Four Nine Six"
            |         }
            |       },
            |       {
            |         "content": {
            |           "__typename": "Comment",
            |           "id": "comment/1234",
            |           "content": "One Two Three Four"
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
