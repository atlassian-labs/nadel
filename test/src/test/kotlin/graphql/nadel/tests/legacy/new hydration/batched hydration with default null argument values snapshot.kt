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
    graphql.nadel.tests.next.update<`batched hydration with default null argument values`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batched hydration with default null argument values snapshot` : TestSnapshot() {
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
                |           "USER-1",
                |           "USER-2"
                |         ],
                |         "id": "ISSUE-1"
                |       },
                |       {
                |         "__typename__batch_hydration__authors": "Issue",
                |         "batch_hydration__authors__authorIds": [
                |           "USER-3"
                |         ],
                |         "id": "ISSUE-2"
                |       },
                |       {
                |         "__typename__batch_hydration__authors": "Issue",
                |         "batch_hydration__authors__authorIds": [
                |           "USER-2",
                |           "USER-4",
                |           "USER-5"
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
                |   usersByIds(id: ["USER-1", "USER-2", "USER-3"], test: null) {
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
                |         "id": "USER-1",
                |         "batch_hydration__authors__id": "USER-1"
                |       },
                |       {
                |         "id": "USER-2",
                |         "batch_hydration__authors__id": "USER-2"
                |       },
                |       {
                |         "id": "USER-3",
                |         "batch_hydration__authors__id": "USER-3"
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
                |   usersByIds(id: ["USER-4", "USER-5"], test: null) {
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
                |         "id": "USER-4",
                |         "batch_hydration__authors__id": "USER-4"
                |       },
                |       {
                |         "id": "USER-5",
                |         "batch_hydration__authors__id": "USER-5"
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
     *             "id": "USER-1"
     *           },
     *           {
     *             "id": "USER-2"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authors": [
     *           {
     *             "id": "USER-3"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-3",
     *         "authors": [
     *           {
     *             "id": "USER-2"
     *           },
     *           {
     *             "id": "USER-4"
     *           },
     *           {
     *             "id": "USER-5"
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
            |             "id": "USER-1"
            |           },
            |           {
            |             "id": "USER-2"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authors": [
            |           {
            |             "id": "USER-3"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-3",
            |         "authors": [
            |           {
            |             "id": "USER-2"
            |           },
            |           {
            |             "id": "USER-4"
            |           },
            |           {
            |             "id": "USER-5"
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
