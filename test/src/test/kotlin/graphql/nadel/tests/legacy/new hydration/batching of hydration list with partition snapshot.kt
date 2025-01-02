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
    graphql.nadel.tests.next.update<`batching of hydration list with partition`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batching of hydration list with partition snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   issues {
                |     __typename__batch_hydration__authors: __typename
                |     batch_hydration__authors__authorIds: authorIds
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
                |         "__typename__batch_hydration__authors": "Issue",
                |         "batch_hydration__authors__authorIds": [
                |           "CLOUD-ID-1/USER-1",
                |           "CLOUD-ID-2/USER-2"
                |         ],
                |         "id": "ISSUE-1"
                |       },
                |       {
                |         "__typename__batch_hydration__authors": "Issue",
                |         "batch_hydration__authors__authorIds": [
                |           "CLOUD-ID-1/USER-3"
                |         ],
                |         "id": "ISSUE-2"
                |       },
                |       {
                |         "__typename__batch_hydration__authors": "Issue",
                |         "batch_hydration__authors__authorIds": [
                |           "CLOUD-ID-2/USER-4",
                |           "CLOUD-ID-1/USER-5"
                |         ],
                |         "id": "ISSUE-3"
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
                | query {
                |   usersByIds(id: ["CLOUD-ID-1/USER-1", "CLOUD-ID-1/USER-3"]) {
                |     id
                |     batch_hydration__authors__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "id": "CLOUD-ID-1/USER-1",
                |         "batch_hydration__authors__id": "CLOUD-ID-1/USER-1"
                |       },
                |       {
                |         "id": "CLOUD-ID-1/USER-3",
                |         "batch_hydration__authors__id": "CLOUD-ID-1/USER-3"
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
                | query {
                |   usersByIds(id: ["CLOUD-ID-1/USER-5"]) {
                |     id
                |     batch_hydration__authors__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "id": "CLOUD-ID-1/USER-5",
                |         "batch_hydration__authors__id": "CLOUD-ID-1/USER-5"
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
                | query {
                |   usersByIds(id: ["CLOUD-ID-2/USER-2", "CLOUD-ID-2/USER-4"]) {
                |     id
                |     batch_hydration__authors__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "id": "CLOUD-ID-2/USER-2",
                |         "batch_hydration__authors__id": "CLOUD-ID-2/USER-2"
                |       },
                |       {
                |         "id": "CLOUD-ID-2/USER-4",
                |         "batch_hydration__authors__id": "CLOUD-ID-2/USER-4"
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
     *     "issues": [
     *       {
     *         "id": "ISSUE-1",
     *         "authors": [
     *           {
     *             "id": "CLOUD-ID-1/USER-1"
     *           },
     *           {
     *             "id": "CLOUD-ID-2/USER-2"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authors": [
     *           {
     *             "id": "CLOUD-ID-1/USER-3"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-3",
     *         "authors": [
     *           {
     *             "id": "CLOUD-ID-2/USER-4"
     *           },
     *           {
     *             "id": "CLOUD-ID-1/USER-5"
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
            |     "issues": [
            |       {
            |         "id": "ISSUE-1",
            |         "authors": [
            |           {
            |             "id": "CLOUD-ID-1/USER-1"
            |           },
            |           {
            |             "id": "CLOUD-ID-2/USER-2"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authors": [
            |           {
            |             "id": "CLOUD-ID-1/USER-3"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-3",
            |         "authors": [
            |           {
            |             "id": "CLOUD-ID-2/USER-4"
            |           },
            |           {
            |             "id": "CLOUD-ID-1/USER-5"
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
