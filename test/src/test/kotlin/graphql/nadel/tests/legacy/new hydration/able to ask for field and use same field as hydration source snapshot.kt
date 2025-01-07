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
    graphql.nadel.tests.next.update<`able to ask for field and use same field as hydration source`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `able to ask for field and use same field as hydration source snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   bar {
                |     __typename__hydration__nestedBar: __typename
                |     barId
                |     hydration__nestedBar__barId: barId
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "bar": {
                |       "barId": "1",
                |       "hydration__nestedBar__barId": "1",
                |       "__typename__hydration__nestedBar": "Bar",
                |       "name": "Test"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   barById(id: "1") {
                |     __typename__hydration__nestedBar: __typename
                |     hydration__nestedBar__barId: barId
                |     barId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barById": {
                |       "hydration__nestedBar__barId": "1",
                |       "__typename__hydration__nestedBar": "Bar",
                |       "barId": "1"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   barById(id: "1") {
                |     barId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barById": {
                |       "barId": "1"
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
     *     "bar": {
     *       "barId": "1",
     *       "name": "Test",
     *       "nestedBar": {
     *         "barId": "1",
     *         "nestedBar": {
     *           "barId": "1"
     *         }
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "bar": {
            |       "barId": "1",
            |       "name": "Test",
            |       "nestedBar": {
            |         "barId": "1",
            |         "nestedBar": {
            |           "barId": "1"
            |         }
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
