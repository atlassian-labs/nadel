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
    graphql.nadel.tests.next.update<CastHydrationLongInputToStringForMatchingTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class CastHydrationLongInputToStringForMatchingTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   someData {
                |     __typename__batch_hydration__spaces: __typename
                |     batch_hydration__spaces__spaceIds: spaceIds
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "someData": {
                |       "batch_hydration__spaces__spaceIds": [
                |         1,
                |         2,
                |         10
                |       ],
                |       "__typename__batch_hydration__spaces": "SomeData"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   spaces(ids: [1, 2, 10]) {
                |     id
                |     batch_hydration__spaces__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "spaces": [
                |       {
                |         "id": "1",
                |         "batch_hydration__spaces__id": "1"
                |       },
                |       {
                |         "id": "2",
                |         "batch_hydration__spaces__id": "2"
                |       },
                |       {
                |         "id": "10",
                |         "batch_hydration__spaces__id": "10"
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
     *     "someData": {
     *       "spaces": [
     *         {
     *           "id": "1"
     *         },
     *         {
     *           "id": "2"
     *         },
     *         {
     *           "id": "10"
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "someData": {
            |       "spaces": [
            |         {
            |           "id": "1"
            |         },
            |         {
            |           "id": "2"
            |         },
            |         {
            |           "id": "10"
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
