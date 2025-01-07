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
    graphql.nadel.tests.next.update<`index hydration with partitioned inputs`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `index hydration with partitioned inputs snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
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
                |         "id": "ISSUE-1",
                |         "batch_hydration__authors__authorIds": [
                |           "site-1/user-1",
                |           "site-2/user-2"
                |         ],
                |         "__typename__batch_hydration__authors": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-2",
                |         "batch_hydration__authors__authorIds": [
                |           "site-1/user-3"
                |         ],
                |         "__typename__batch_hydration__authors": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-3",
                |         "batch_hydration__authors__authorIds": [
                |           "site-2/user-4",
                |           "site-1/user-5"
                |         ],
                |         "__typename__batch_hydration__authors": "Issue"
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
                | {
                |   usersByIds(id: ["site-1/user-1", "site-1/user-3"]) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "id": "site-1/user-1"
                |       },
                |       {
                |         "id": "site-1/user-3"
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
                | {
                |   usersByIds(id: ["site-1/user-5"]) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "id": "site-1/user-5"
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
                | {
                |   usersByIds(id: ["site-2/user-2", "site-2/user-4"]) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "id": "site-2/user-2"
                |       },
                |       {
                |         "id": "site-2/user-4"
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
     *             "id": "site-1/user-1"
     *           },
     *           {
     *             "id": "site-2/user-2"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authors": [
     *           {
     *             "id": "site-1/user-3"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-3",
     *         "authors": [
     *           {
     *             "id": "site-2/user-4"
     *           },
     *           {
     *             "id": "site-1/user-5"
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
            |             "id": "site-1/user-1"
            |           },
            |           {
            |             "id": "site-2/user-2"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authors": [
            |           {
            |             "id": "site-1/user-3"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-3",
            |         "authors": [
            |           {
            |             "id": "site-2/user-4"
            |           },
            |           {
            |             "id": "site-1/user-5"
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
