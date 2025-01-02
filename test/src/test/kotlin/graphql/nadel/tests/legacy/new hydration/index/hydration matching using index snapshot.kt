// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.index

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration matching using index`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration matching using index snapshot` : TestSnapshot() {
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
                |           "1"
                |         ],
                |         "__typename__batch_hydration__authors": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-2",
                |         "batch_hydration__authors__authorIds": [
                |           "1",
                |           "2"
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
                |   usersByIds(ids: ["1", "2"]) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "name": "Name"
                |       },
                |       {
                |         "name": "Name 2"
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
     *             "name": "Name"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authors": [
     *           {
     *             "name": "Name"
     *           },
     *           {
     *             "name": "Name 2"
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
            |             "name": "Name"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authors": [
            |           {
            |             "name": "Name"
            |           },
            |           {
            |             "name": "Name 2"
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
