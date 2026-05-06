// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<ScalarIndexHydrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class ScalarIndexHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issues {
                |     __typename__batch_hydration__linkedCount: __typename
                |     id
                |     batch_hydration__linkedCount__id: id
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "id": "1",
                |         "key": "GQLGW-1",
                |         "batch_hydration__linkedCount__id": "1",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "2",
                |         "key": "GQLGW-2",
                |         "batch_hydration__linkedCount__id": "2",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "3",
                |         "key": "GQLGW-3",
                |         "batch_hydration__linkedCount__id": "3",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "4",
                |         "key": "GQLGW-4",
                |         "batch_hydration__linkedCount__id": "4",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "5",
                |         "key": "GQLGW-5",
                |         "batch_hydration__linkedCount__id": "5",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "6",
                |         "key": "GQLGW-6",
                |         "batch_hydration__linkedCount__id": "6",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "7",
                |         "key": "GQLGW-7",
                |         "batch_hydration__linkedCount__id": "7",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "8",
                |         "key": "GQLGW-8",
                |         "batch_hydration__linkedCount__id": "8",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "9",
                |         "key": "GQLGW-9",
                |         "batch_hydration__linkedCount__id": "9",
                |         "__typename__batch_hydration__linkedCount": "Issue"
                |       },
                |       {
                |         "id": "10",
                |         "key": "GQLGW-10",
                |         "batch_hydration__linkedCount__id": "10",
                |         "__typename__batch_hydration__linkedCount": "Issue"
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
                |   linkedCounts(ids: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"])
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "linkedCounts": [
                |       2,
                |       4,
                |       6,
                |       8,
                |       10,
                |       12,
                |       14,
                |       16,
                |       18,
                |       20
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
     *         "id": "1",
     *         "key": "GQLGW-1",
     *         "linkedCount": 2
     *       },
     *       {
     *         "id": "2",
     *         "key": "GQLGW-2",
     *         "linkedCount": 4
     *       },
     *       {
     *         "id": "3",
     *         "key": "GQLGW-3",
     *         "linkedCount": 6
     *       },
     *       {
     *         "id": "4",
     *         "key": "GQLGW-4",
     *         "linkedCount": 8
     *       },
     *       {
     *         "id": "5",
     *         "key": "GQLGW-5",
     *         "linkedCount": 10
     *       },
     *       {
     *         "id": "6",
     *         "key": "GQLGW-6",
     *         "linkedCount": 12
     *       },
     *       {
     *         "id": "7",
     *         "key": "GQLGW-7",
     *         "linkedCount": 14
     *       },
     *       {
     *         "id": "8",
     *         "key": "GQLGW-8",
     *         "linkedCount": 16
     *       },
     *       {
     *         "id": "9",
     *         "key": "GQLGW-9",
     *         "linkedCount": 18
     *       },
     *       {
     *         "id": "10",
     *         "key": "GQLGW-10",
     *         "linkedCount": 20
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
            |         "id": "1",
            |         "key": "GQLGW-1",
            |         "linkedCount": 2
            |       },
            |       {
            |         "id": "2",
            |         "key": "GQLGW-2",
            |         "linkedCount": 4
            |       },
            |       {
            |         "id": "3",
            |         "key": "GQLGW-3",
            |         "linkedCount": 6
            |       },
            |       {
            |         "id": "4",
            |         "key": "GQLGW-4",
            |         "linkedCount": 8
            |       },
            |       {
            |         "id": "5",
            |         "key": "GQLGW-5",
            |         "linkedCount": 10
            |       },
            |       {
            |         "id": "6",
            |         "key": "GQLGW-6",
            |         "linkedCount": 12
            |       },
            |       {
            |         "id": "7",
            |         "key": "GQLGW-7",
            |         "linkedCount": 14
            |       },
            |       {
            |         "id": "8",
            |         "key": "GQLGW-8",
            |         "linkedCount": 16
            |       },
            |       {
            |         "id": "9",
            |         "key": "GQLGW-9",
            |         "linkedCount": 18
            |       },
            |       {
            |         "id": "10",
            |         "key": "GQLGW-10",
            |         "linkedCount": 20
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
