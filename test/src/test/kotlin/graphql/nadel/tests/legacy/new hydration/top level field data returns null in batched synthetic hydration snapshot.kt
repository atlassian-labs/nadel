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
    graphql.nadel.tests.next.update<`top level field data returns null in batched synthetic hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `top level field data returns null in batched synthetic hydration snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service1",
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
                |           "USER-1",
                |           "USER-2"
                |         ],
                |         "__typename__batch_hydration__authors": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-2",
                |         "batch_hydration__authors__authorIds": [
                |           "USER-3"
                |         ],
                |         "__typename__batch_hydration__authors": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-3",
                |         "batch_hydration__authors__authorIds": [
                |           "USER-2",
                |           "USER-4",
                |           "USER-5"
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
                service = "service2",
                query = """
                | {
                |   users {
                |     usersByIds(id: ["USER-1", "USER-2", "USER-3"]) {
                |       id
                |       batch_hydration__authors__id: id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": {
                |       "usersByIds": null
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service2",
                query = """
                | {
                |   users {
                |     usersByIds(id: ["USER-4", "USER-5"]) {
                |       id
                |       batch_hydration__authors__id: id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": {
                |       "usersByIds": null
                |     }
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
     *           null,
     *           null
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authors": [
     *           null
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-3",
     *         "authors": [
     *           null,
     *           null,
     *           null
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
            |           null,
            |           null
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authors": [
            |           null
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-3",
            |         "authors": [
            |           null,
            |           null,
            |           null
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
