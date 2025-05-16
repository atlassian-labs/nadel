// @formatter:off
package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationFollowedByPartitionTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class HydrationFollowedByPartitionTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "activities_service",
                query = """
                | query getViewedVideos {
                |   viewed {
                |     __typename__batch_hydration__data: __typename
                |     batch_hydration__data__dataId: dataId
                |     type
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "viewed": [
                |       {
                |         "type": "video",
                |         "batch_hydration__data__dataId": "video-1:partition-A",
                |         "__typename__batch_hydration__data": "ActivityItem"
                |       },
                |       {
                |         "type": "video",
                |         "batch_hydration__data__dataId": "video-2:partition-B",
                |         "__typename__batch_hydration__data": "ActivityItem"
                |       },
                |       {
                |         "type": "video",
                |         "batch_hydration__data__dataId": "video-3:partition-A",
                |         "__typename__batch_hydration__data": "ActivityItem"
                |       },
                |       {
                |         "type": "video",
                |         "batch_hydration__data__dataId": "video-4:partition-B",
                |         "__typename__batch_hydration__data": "ActivityItem"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "videos_service",
                query = """
                | query getViewedVideos {
                |   videos(ids: ["video-1:partition-A", "video-3:partition-A"]) {
                |     id
                |     batch_hydration__data__id: id
                |     title
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "videos": [
                |       {
                |         "id": "video-1:partition-A",
                |         "title": "VIDEO-1:PARTITION-A",
                |         "batch_hydration__data__id": "video-1:partition-A"
                |       },
                |       {
                |         "id": "video-3:partition-A",
                |         "title": "VIDEO-3:PARTITION-A",
                |         "batch_hydration__data__id": "video-3:partition-A"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "videos_service",
                query = """
                | query getViewedVideos {
                |   videos(ids: ["video-2:partition-B", "video-4:partition-B"]) {
                |     id
                |     batch_hydration__data__id: id
                |     title
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "videos": [
                |       {
                |         "id": "video-2:partition-B",
                |         "title": "VIDEO-2:PARTITION-B",
                |         "batch_hydration__data__id": "video-2:partition-B"
                |       },
                |       {
                |         "id": "video-4:partition-B",
                |         "title": "VIDEO-4:PARTITION-B",
                |         "batch_hydration__data__id": "video-4:partition-B"
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
     *     "viewed": [
     *       {
     *         "type": "video",
     *         "data": {
     *           "id": "video-1:partition-A",
     *           "title": "VIDEO-1:PARTITION-A"
     *         }
     *       },
     *       {
     *         "type": "video",
     *         "data": {
     *           "id": "video-2:partition-B",
     *           "title": "VIDEO-2:PARTITION-B"
     *         }
     *       },
     *       {
     *         "type": "video",
     *         "data": {
     *           "id": "video-3:partition-A",
     *           "title": "VIDEO-3:PARTITION-A"
     *         }
     *       },
     *       {
     *         "type": "video",
     *         "data": {
     *           "id": "video-4:partition-B",
     *           "title": "VIDEO-4:PARTITION-B"
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
            |     "viewed": [
            |       {
            |         "type": "video",
            |         "data": {
            |           "id": "video-1:partition-A",
            |           "title": "VIDEO-1:PARTITION-A"
            |         }
            |       },
            |       {
            |         "type": "video",
            |         "data": {
            |           "id": "video-2:partition-B",
            |           "title": "VIDEO-2:PARTITION-B"
            |         }
            |       },
            |       {
            |         "type": "video",
            |         "data": {
            |           "id": "video-3:partition-A",
            |           "title": "VIDEO-3:PARTITION-A"
            |         }
            |       },
            |       {
            |         "type": "video",
            |         "data": {
            |           "id": "video-4:partition-B",
            |           "title": "VIDEO-4:PARTITION-B"
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
